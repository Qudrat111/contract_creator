package uz.backend.contract_creator

import org.apache.poi.xwpf.usermodel.*
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
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
    fun getOneUser(userId: Long): UserDTO
    fun givePermission(userId: Long, contractId: Long)

}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val contractRepository: ContractRepository
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

    override fun getOneUser(userId: Long): UserDTO {
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()
        return UserDTO.toResponse(user)
    }

    override fun givePermission(userId: Long, contractId: Long) {
        val contract = contractRepository.findByIdAndDeletedFalse(contractId) ?: throw ContractNotFoundException()
        if (userRepository.existsById(userId)) contract.allowedOperators.add(userId)
        else throw UserNotFoundException()
    }
}


@Service
class DocFileService(
    private val templateRepository: TemplateRepository,
    private val contractRepository: ContractRepository,
    private val fieldRepository: FieldRepository,
    private val contractFieldValueRepository: ContractFieldValueRepository
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

            processDocument(document, keyValueMap)

            FileOutputStream(outputFilePath).use { outputStream ->
                document.write(outputStream)
            }
            document.close()
        }
    }


    private fun processParagraph(paragraph: XWPFParagraph, keyValueMap: Map<String, String>): String? {
        paragraph.run {
            val firstIndex = text.indexOf("##") + 2
            if (firstIndex > 1) {
                val lastIndex = text.indexOf("##", firstIndex)
                if (lastIndex > -1) {
                    val key = text.substring(firstIndex, lastIndex)
                    keyValueMap[key]?.let { value ->
                        var newText: String? =
                            text.substring(0, firstIndex - 2) + value + text.substring(lastIndex + 2)
                        if (text.indexOf("##", lastIndex + 2) > -1) {
                            newText = processParagraph(this, keyValueMap)
                        }
                        for (run in paragraph.runs) {
                            run.setText("", 0)
                        }
                        paragraph.createRun().setText(newText)
                        return newText
                    }
                }
                return text
            }
        }
        return null
    }

    private fun processDocument(document: XWPFDocument, keyValueMap: Map<String, String>) {
        document.run {
            paragraphs.forEach { processParagraph(it, keyValueMap) }

            tables.forEach { table ->
                table.rows.forEach { row ->
                    row.tableCells.forEach { cell ->
                        cell.paragraphs.forEach { paragraph ->
                            processParagraph(
                                paragraph,
                                keyValueMap
                            )
                        }
                    }
                }
            }
            headerList.forEach { header ->
                header.paragraphs.forEach { processParagraph(it, keyValueMap) }
            }
            footerList.forEach { footer ->
                footer.paragraphs.forEach { processParagraph(it, keyValueMap) }
            }
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

    //    fun getAllTemplates(): List<TemplateDto> {
//        val listTemplates = mutableListOf<TemplateDto>()
//        templateRepository.findAllNotDeleted().let {
//            it.forEach { template ->
//                listTemplates.add(TemplateDto())
//            }
//        }
//        return listTemplates}

    fun createNewTemplate(file: MultipartFile, name: String) {
        val filename = file.originalFilename!!.substringBeforeLast(".") + "-" + UUID.randomUUID() + ".docx"
        val filePath = "./files/templates/$filename"
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, Paths.get(filePath))
        }
        val keys = getKeys(filePath)
        val fields = getFieldsByKeys(keys)
        templateRepository.save(Template(name, filePath, fields))
    }

    fun deleteTemplate(id: Long) {
        templateRepository.trash(id)
    }

    fun getOneTemplate(id: Long): XWPFDocument? {
        return templateRepository.findByIdAndDeletedFalse(id)?.let {
            val file = FileInputStream(it.filePath)
            val document = XWPFDocument(file)
            return document
        }
    }

    private fun getFieldsByKeys(keys: MutableList<String>): List<Field> {
        return keys.map {
            fieldRepository.findByName(it) ?: run {
                fieldRepository.save(Field(it, TypeEnum.STRING))
            }
        }
    }

    fun downloadContract(downloadContractDTO: DownloadContractDTO): ResponseEntity<Resource> {
        downloadContractDTO.let {
            contractRepository.findByIdAndDeletedFalse(it.contractId)?.let { contract ->
                contract.run {
//                    var filePathStr = contractFilePath.substringBeforeLast(".")
//                    val fileType = when (it.fileType.lowercase()) {
//                        "pdf" -> "pdf"
//                        "docx" -> "docx"
//                        else -> throw RuntimeException("invalid file type")
//                    }
//                    filePathStr = "$filePathStr.$fileType"

                    val filePath = Paths.get(contractFilePath)
                    val resource = UrlResource(filePath.toUri())

                    if (resource.exists() && resource.isReadable) {
                        return ResponseEntity.ok()
                            .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"contract_${it.contractId}.docx\""
                            )
                            .body(resource)
                    }
                }
            }
        }
        throw RuntimeException("something went wrong")
    }

    fun addContract(createContractDTO: CreateContractDTO): Contract {
        createContractDTO.run {
            templateRepository.findByIdAndDeletedFalse(templateId)?.let { template ->
                template.let { it ->
                    val fileName = it.filePath.substringAfterLast("/")
                    val contractFilePathDocx = "./files/contracts/${fileName}"
                    Files.copy(Paths.get(it.filePath), Paths.get(contractFilePathDocx))

                    changeAllKeysToValues(templateId, contractFilePathDocx, fields)
                    val contract = contractRepository.save(Contract(it, clientPassport, contractFilePathDocx))

                    val contractFieldValueMap = fields.map { fieldEntry ->
                        val fieldOpt = fieldRepository.findByName(fieldEntry.key)
                        fieldOpt?.let { field -> ContractFieldValue(contract, field, fieldEntry.value) }
                    }
                    contractFieldValueRepository.saveAll(contractFieldValueMap)

//                    fileName = fileName.substringBeforeLast(".")
//                    val contractFilePathPdf = "./files/contracts/${fileName}.pdf"
//                    convertWordToPdf(
//                        Files.newInputStream(Paths.get(contractFilePathDocx)),
//                        Files.newOutputStream(Paths.get(contractFilePathPdf))
//                    )
//                    Files.copy(Paths.get(it.filePath), Paths.get(contractFilePathDocx))

                    return contract
                }
            }
        }
        throw RuntimeException("template not found")
    }


    private fun convertWordToPdf(inputStream: InputStream, outputStream: OutputStream) {
        val wordMLPackage = WordprocessingMLPackage.load(inputStream)
        Docx4J.toPDF(wordMLPackage, outputStream)
    }

    private fun getKey(run: XWPFRun): String? {
        return getKey(run.text())
    }

    private fun getKey(text: String): String? {
        if (text.contains("##")) {
            val firstIndex = text.indexOf("##") + 2
            if (text.substring(firstIndex).contains("##")) {
                val lastIndex = text.indexOf("##", firstIndex)
                return text.substring(firstIndex, lastIndex)
            }
        }
        return null
    }

    private fun getKey(run: XWPFTableCell): String? {
        return getKey(run.text)
    }

    private fun getKeys(filePath: String): MutableList<String> {
        val document = readDocFile(filePath)
        val keys = mutableListOf<String>()
        for (table in document.tables)
            keys.addAll(getKeys(table))
        keys.addAll(getKeys(document.paragraphs))
        return keys
    }

    private fun getKeys(table: XWPFTable): MutableList<String> {
        val keys = mutableListOf<String>()
        for (row in table.rows)
            for (tableCell in row.tableCells) {
                getKey(tableCell)?.let { keys.add(it) }
            }
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
            getKey(run)?.let { keys.add(it) }
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