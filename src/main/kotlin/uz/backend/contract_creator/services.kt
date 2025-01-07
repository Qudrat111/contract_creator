package uz.backend.contract_creator

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.io.FileInputStream
import java.io.FileOutputStream

interface AuthService : UserDetailsService {
    fun logIn(signInDTO: LogInDTO): TokenDTO?

}

@Service
class AuthServiceImpl(
    private val authenticationProvider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) : AuthService {
    override fun logIn(signInDTO: LogInDTO): TokenDTO? {
        val authentication =
            UsernamePasswordAuthenticationToken(signInDTO.username, signInDTO.password)

        authenticationProvider.authenticate(authentication)

        val user = loadUserByUsername(signInDTO.username)

        val matches: Boolean = passwordEncoder.matches(signInDTO.password, user.password)

        if (!matches) throw UserNotFoundException()

        val token: String = jwtProvider.generateToken(signInDTO.username)

        return TokenDTO(token)
    }

    override fun loadUserByUsername(username: String): UserDetails {

        return userRepository.findByUsername(username) ?: throw RuntimeException("User $username not found")
    }
}

@Service
class AttachService {
    fun saveToSystem(file: MultipartFile): String? {
        val folder = File("Files")
        if (!folder.exists()) folder.mkdir()

        val bytes = file.bytes
        val path: Path = Paths.get("Files/" + file.originalFilename)
        Files.write(path, bytes)
        return file.originalFilename
    }
}


@Service
class DocFileService {
    fun readDocFile(filePath: String):XWPFDocument{
        FileInputStream(filePath).use { inputStream ->
            return XWPFDocument(inputStream)
        }
    }

    fun changeAllKeysToValues(filePath: String, keyValueMap: Map<String, String>) {
        val document = readDocFile(filePath)
        val paragraphs = document.paragraphs
        for (paragraph in paragraphs)
            for (run in paragraph.runs)
                if (run != null)
                    processRun(run, keyValueMap)
        FileOutputStream(filePath).use { outputStream ->
            document.write(outputStream)
        }
        document.close()
    }
    private fun processRun(run: XWPFRun, keyValueMap: Map<String, String>):String? {
        val text = run.text()
        if (text.contains("##")) {
            val firstIndex = text.indexOf("##") + 2
            if (text.indexOf("##", firstIndex) > -1) {
                val lastIndex = text.indexOf("##", firstIndex)

                val key = text.substring(firstIndex, lastIndex)
                val valueOpt = keyValueMap[key]
                valueOpt?.let { value ->
                    var newText: String? = text.substring(0, firstIndex - 2) + value + text.substring(lastIndex + 2)
                    if (text.substring(lastIndex + 2).contains("##")) {
                        run.setText(newText)
                        newText = processRun(run, keyValueMap)
                    }
                    println(newText)
                    return newText
                } ?: run { return text }
            } else return text
        }
        return null
    }

    fun getKey(run: XWPFRun): String? {
        var keyTemp = run.text()
        if (keyTemp.contains("##")) {
            val firstIndex = keyTemp.indexOf("##") + 2
            keyTemp = keyTemp.substring(firstIndex)
            if (keyTemp.contains("##")) {
                val lastIndex = keyTemp.indexOf("##") + keyTemp.length + 2
                keyTemp = run.text().substring(firstIndex, lastIndex)
                return keyTemp
            }
        }
        return null
    }

    fun getKeys(paragraphs: List<XWPFParagraph>): MutableList<String> {
        val keys = mutableListOf<String>()
        for (paragraph in paragraphs)
            keys.addAll(getKeys(paragraph))
        return keys
    }

    fun getKeys(paragraph: XWPFParagraph): MutableList<String> {
        val keys = mutableListOf<String>()
        for (run in paragraph.runs) {
            val key = getKey(run)
            if (key != null)
                keys.add(key)
        }
        return keys
    }
}