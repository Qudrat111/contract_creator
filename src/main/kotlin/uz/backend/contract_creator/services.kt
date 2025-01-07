package uz.backend.contract_creator

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sun.security.jgss.GSSUtil.login
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


class DocFileService {
    private fun processRun(run: XWPFRun, keyValueMap: Map<String, String>) {
        var keyTemp = run.text()
        if (keyTemp.contains("##")) {
            val firstIndex = keyTemp.indexOf("##") + 2
            keyTemp = keyTemp.substring(firstIndex)
            if (keyTemp.contains("##")) {
                val lastIndex = keyTemp.indexOf("##") + keyTemp.length + 2
                keyTemp = run.text().substring(firstIndex, lastIndex)


                //TODO get value by key
                val key = keyTemp
                val valueOpt = keyValueMap[key]
                valueOpt?.let { value ->
                    if (run.text().substring(lastIndex).contains("##"))
                        processRun(run, keyValueMap)

                    val newText = run.text().substring(0, firstIndex - 2) + value + run.text().substring(lastIndex + 2)
                    run.setText(newText, 0)
                }

            }
        }
    }

    fun changeAllKeysToValues(filePath: String, keyValueMap: Map<String, String>) {
        FileInputStream(filePath).use { inputStream ->
            val document = XWPFDocument(inputStream)
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