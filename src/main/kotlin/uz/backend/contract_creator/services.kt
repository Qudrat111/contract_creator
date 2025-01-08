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
import java.nio.file.Paths
import java.util.*

interface AuthService : UserDetailsService {

    fun logIn(signInDTO: LogInDTO): String
    fun signIn(signInDTO: SignInDTO): UserDTO

}

interface FieldService {
    fun createField(dto: FieldDTO)
    fun getFieldById(id: Long): FieldDTO
    fun getAllField(): List<FieldDTO>
    fun updateField(id: Long, updateDto: FieldUpdateDTO)
    fun deleteField(id: Long)

}

@Service
class AuthServiceImpl(
    private val authenticationProvider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) : AuthService {
    override fun logIn(signInDTO: LogInDTO): String {
        val authentication =
            UsernamePasswordAuthenticationToken(signInDTO.username, signInDTO.password)

        authenticationProvider.authenticate(authentication)

        val user = loadUserByUsername(signInDTO.username)

        val matches: Boolean = passwordEncoder.matches(signInDTO.password, user.password)

        if (!matches) throw UserNotFoundException()

        val token: String = jwtProvider.generateToken(signInDTO.username)

        return token
    }

    override fun signIn(signInDTO: SignInDTO): UserDTO {
        return signInDTO.run {
            val encoded = passwordEncoder.encode(signInDTO.password)
            this.password = encoded
            UserDTO.toResponse(userRepository.save(this.toEntity()))
        }
    }


    override fun loadUserByUsername(username: String): UserDetails {

        return userRepository.findByUserName(username) ?: throw UserNotFoundException()
    }
}

interface UserService {
    fun changeRole(userId: Long, role: RoleEnum): UserDTO
    fun getAllUsers(): List<UserDTO>

}

class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    override fun changeRole(userId: Long, role: RoleEnum): UserDTO {
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()
        user.role = role
        return UserDTO.toResponse(userRepository.save(user))
    }

    override fun getAllUsers(): List<UserDTO> {
        return userRepository.findAllNotDeleted().map {
            UserDTO.toResponse(it)
        }
    }
}


@Service
class DocFileService(
    private val templateRepository: TemplateRepository,
    private val contractRepository: ContractRepository
) {
    private fun readDocFile(filePath: String): XWPFDocument {
        FileInputStream(filePath).use { inputStream ->
            return XWPFDocument(inputStream)
        }
    }

    private fun changeAllKeysToValues(templateId: Long, outputFilePath: String, keyValueMap: Map<String, String>) {
        val templateOpt = templateRepository.findByIdAndDeletedFalse(templateId)
        templateOpt?.let { template ->
            val filePath = template.filePath
            val document = readDocFile(filePath)
            val paragraphs = document.paragraphs
            for (paragraph in paragraphs)
                for (run in paragraph.runs)
                    if (run != null)
                        processRun(run, keyValueMap)
            FileOutputStream(outputFilePath).use { outputStream ->
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

    fun createNewTemplate(file: MultipartFile, name: String) {
        val filePath = "./files/templates/${file.originalFilename}-" + UUID.randomUUID()
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, Paths.get(filePath))
        }
        val keys = getKeys(filePath)
        val fields = getFieldsByKeys(keys)
        templateRepository.save(Template(name, filePath, fields))
    }

    private fun getFieldsByKeys(keys: MutableList<String>): List<Field> {
        return keys.map { Field(it, TypeEnum.STRING) }
    }

    fun downloadContract(downloadContractDTO: DownloadContractDTO): ResponseEntity<Resource> {
        downloadContractDTO.let {
            contractRepository.findByIdAndDeletedFalse(it.contractId)?.let { contract ->
                contract.run {
                    var filePathStr = contractFilePath.substringBeforeLast(".")
                    val fileType = when (it.fileType.lowercase()) {
                        "pdf" -> "pdf"
                        "docx" -> "docx"
                        else -> throw RuntimeException("invalid file type")
                    }
                    filePathStr = "$filePathStr.$fileType"
                    val filePath = Paths.get(filePathStr)
                    val resource = UrlResource(filePath.toUri())

                    if (resource.exists() && resource.isReadable) {
                        return ResponseEntity.ok()
                            .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"contract_${it.contractId}.${fileType}\""
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
                    var fileName = it.filePath.substringAfterLast("/")
                    val contractFilePathDocx = "./files/contracts/${fileName}"
                    Files.copy(Paths.get(it.filePath), Paths.get(contractFilePathDocx))

                    changeAllKeysToValues(templateId, contractFilePathDocx, fields)

                    fileName = fileName.substringBeforeLast(".")
                    val contractFilePathPdf = "./files/contracts/${fileName}.pdf"
                    convertWordToPdf(
                        Files.newInputStream(Paths.get(contractFilePathDocx)),
                        Files.newOutputStream(Paths.get(contractFilePathPdf))
                    )
//                    Files.copy(Paths.get(it.filePath), Paths.get(contractFilePathDocx))

                    contractRepository.save(Contract(it, clientPassport, contractFilePathDocx))
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

@Service
class FieldServiceImpl(
    private val fieldRepository: FieldRepository,
) : FieldService {
    override fun createField(dto: FieldDTO) {
        dto.run {
            if (fieldRepository.existsByName(name)) throw ExistsFieldException()
            fieldRepository.saveAndRefresh(FieldDTO.toEntity(name, type))
        }
    }

    override fun getFieldById(id: Long): FieldDTO {
        return fieldRepository.findByIdAndDeletedFalse(id)?.let { FieldDTO.toDTO(it) } ?: throw FieldNotFoundException()
    }

    override fun getAllField(): List<FieldDTO> {
        return fieldRepository.findAllNotDeleted().map { FieldDTO.toDTO(it) }
    }

    override fun updateField(id: Long, updateDto: FieldUpdateDTO) {
        val field = fieldRepository.findByIdAndDeletedFalse(id) ?: throw FieldNotFoundException()
        updateDto.run {
            name?.let {
                if (fieldRepository.existsByName(name)) throw ExistsFieldException()
                field.name = it
            }
            type?.let { field.type = TypeEnum.valueOf(it.uppercase()) }
        }
        fieldRepository.saveAndRefresh(field)
    }

    override fun deleteField(id: Long) {
        fieldRepository.trash(id) ?: FieldNotFoundException()
    }
}