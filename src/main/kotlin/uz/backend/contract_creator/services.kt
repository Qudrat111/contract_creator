package uz.backend.contract_creator

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.docx4j.Docx4J
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

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
class DocFileService(
    private val templateRepository: TemplateRepository,
    private val contractRepository: ContractRepository
) {
    fun readDocFile(filePath: String): XWPFDocument {
        FileInputStream(filePath).use { inputStream ->
            return XWPFDocument(inputStream)
        }
    }

    fun changeAllKeysToValues(templateId: Long, keyValueMap: Map<String, String>) {
        val templateOpt = templateRepository.findByIdAndDeletedFalse(templateId)
        templateOpt?.let { template ->
            val outputFilePath = "./file/contracts/"
//            file.inputStream.use { inputStream ->
//                Files.copy(inputStream, Paths.get(filePath))
//            }
            val filePath = template.filePath
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
    }

    fun getKeysByTemplateId(templateId: Long): List<String> {
        val keys = mutableListOf<String>()
        templateRepository.findByIdAndDeletedFalse(templateId)?.let { template ->
            for (field in template.fields)
                keys.add(field.name)
        }
        return keys
    }

    // @RequestParam("file") MultipartFile file
    fun createNewTemplate(file: MultipartFile, name: String) {
        val filePath = "./files/templates/${file.originalFilename}-" + UUID.randomUUID()
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, Paths.get(filePath))
        }
        val keys = getKeys(filePath)
        val fields = getFieldsByKeys(keys)
        Template(name, filePath, fields)
    }

    fun getFieldsByKeys(keys: MutableList<String>): List<Field> {
        return keys.map { Field(it, TypeEnum.STRING) }
    }

    fun downloadContract(downloadContractDTO: DownloadContractDTO): ResponseEntity<Resource> {
        downloadContractDTO.run {
            contractRepository.findByIdAndDeletedFalse(contractId)?.let { contract ->
                contract.run {
                    val filePath = Paths.get(contractFilePath)
                    val resource = UrlResource(filePath.toUri())

                    //TODO
                    if (resource.exists() && resource.isReadable) {
                        return ResponseEntity.ok()
                            .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"contract_$contractId.pdf\""
                            )
                            .body(resource)
                    }
                }
            }
        }
        throw RuntimeException("something went wrong")
    }

    fun addContract(addContractDTO: AddContractDTO) {
        addContractDTO.run {
            templateRepository.findByIdAndDeletedFalse(templateId)?.let { template ->
                template.let {
                    val fileName = it.filePath.substringAfterLast("/")
                    val contractFilePath = "./files/contracts/${fileName}-" + UUID.randomUUID()
//                    file.inputStream.use { inputStream ->
//                        Files.copy(inputStream, Paths.get(filePath))
//                    }
                }
            }
        }
    }

    private fun convertWordToPdf(inputStream: InputStream, outputStream: OutputStream) {
        val wordMLPackage = WordprocessingMLPackage.load(inputStream)
        Docx4J.toPDF(wordMLPackage, outputStream)
    }


    private fun processRun(run: XWPFRun, keyValueMap: Map<String, String>): String? {
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

    private fun getKey(run: XWPFRun): String? {
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

    private fun getKeys(filePath: String): MutableList<String> {
        val document = readDocFile(filePath)
        val keys = mutableListOf<String>()
        for (paragraph in document.paragraphs)
            keys.addAll(getKeys(paragraph))
        return keys
    }

    private fun getKeys(paragraphs: List<XWPFParagraph>): MutableList<String> {
        val keys = mutableListOf<String>()
        for (paragraph in paragraphs)
            keys.addAll(getKeys(paragraph))
        return keys
    }

    private fun getKeys(paragraph: XWPFParagraph): MutableList<String> {
        val keys = mutableListOf<String>()
        for (run in paragraph.runs) {
            val key = getKey(run)
            if (key != null)
                keys.add(key)
        }
        return keys
    }
}