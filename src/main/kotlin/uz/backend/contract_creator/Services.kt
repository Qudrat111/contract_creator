package uz.backend.contract_creator

import jakarta.transaction.Transactional
import org.apache.poi.xwpf.usermodel.*
//import org.docx4j.Docx4J
//import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.apache.poi.xwpf.usermodel.XWPFDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolute
import kotlin.io.path.name


interface AuthService : UserDetailsService {

    fun logIn(signInDTO: LogInDTO): String
    fun signIn(signInDTO: SignInDTO): UserDTO

}

interface FieldService {
    fun createField(dto: FieldDTO)
    fun getFieldById(id: Long): FieldGetDto
    fun getAllField(): List<FieldGetDto>
    fun updateField(id: Long, updateDto: FieldUpdateDTO)
    fun deleteField(id: Long)

}

@Service
class AuthServiceImpl(
    private val authenticationProvider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) : AuthService {
    override fun logIn(signInDTO: LogInDTO): String {
        val authentication = UsernamePasswordAuthenticationToken(signInDTO.username, signInDTO.password)

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
    private val contractRepository: ContractRepository,
    private val contractAllowedUserRepository: ContactAllowedUserRepository,
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
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()
        contractAllowedUserRepository.save(ContractAllowedUser(user, contract))
    }
}


@Service
class DocFileService(
    private val templateRepository: TemplateRepository,
    private val contractRepository: ContractRepository,
    private val fieldRepository: FieldRepository,
    private val contractFieldValueRepository: ContractFieldValueRepository,
    private val userRepository: UserRepository,
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
                        val newText: String =
                            text.substring(0, firstIndex - 2) + value + text.substring(lastIndex + 2)
                        for (run in paragraph.runs) {
                            run.setText("", 0)
                        }
                        paragraph.createRun().setText(newText)
                        if (newText.contains("##")) {
                            processParagraph(paragraph, keyValueMap)
                        }
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
            for (field in template.fields) keys.add(field.name)
        }
        return keys
    }

    fun createNewTemplate(file: MultipartFile, name: String): TemplateResponseDto {
        val filename = file.originalFilename!!.substringBeforeLast(".") + "-" + UUID.randomUUID() + ".docx"
        val filePath = "./files/templates/$filename"
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, Paths.get(filePath))
        }
        val keys = getKeys(filePath)
        val fields = getFieldsByKeys(keys)
        return templateRepository
            .save(Template(name, filePath, fields.toMutableList()))
            .toResponseDto()
    }

    fun deleteTemplate(id: Long) {
        templateRepository.trash(id)
    }

    fun getOneTemplate(id: Long): ResponseEntity<Resource>? {
        return templateRepository.findByIdAndDeletedFalse(id)?.let {
            getResource(it.filePath)
        }
    }

    fun getAllTemplates(): List<TemplateDto> {
        val listTemplates = mutableListOf<TemplateDto>()
        templateRepository.findAllNotDeleted().let {
            it.forEach { template ->
                listTemplates.add(TemplateDto.toResponse(template))
            }
        }
        return listTemplates
    }


    private fun getFieldsByKeys(keys: MutableList<String>): List<Field> {
        return keys.map {
            fieldRepository.findByName(it) ?: run {
                fieldRepository.save(Field(it, TypeEnum.STRING))
            }
        }
    }

    fun downloadContract(downloadContractDTO: DownloadContractDTO): ResponseEntity<Resource> {
        val filesToZip = mutableListOf<String>()

        for (contractId in downloadContractDTO.contractIds) {
            downloadContractDTO.let {
                contractRepository.findByIdAndDeletedFalse(contractId)?.let { contract ->
                    contract.run {
                        var filePathStr = contractFilePath.substringBeforeLast(".")
                        val fileType = when (it.fileType.lowercase()) {
                            "pdf" -> "pdf"
                            "docx" -> "docx"
                            else -> throw RuntimeException("invalid file type")
                        }
                        filePathStr = "$filePathStr.$fileType"
                        filesToZip.add(filePathStr)
                    }
                }
            }
            throw RuntimeException("something went wrong")
        }
        val zipFileName = "./files/zips/${UUID.randomUUID()}.zip"

        ZipOutputStream(FileOutputStream(zipFileName)).use { zipOut ->
            filesToZip.forEach { fileName ->
                val fileToZip = File(fileName)
                zipOut.putNextEntry(ZipEntry(fileToZip.name))
                fileToZip.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
        val filePath = Paths.get(zipFileName)
        val resource = UrlResource(filePath.toUri())

        if (resource.exists() && resource.isReadable) {
            return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"contracts.zip\""
            ).body(resource)
        }

        return ResponseEntity.ok().build()
    }

    @Transactional
    fun addContract(createContractDTOs: List<AddContractDTO>): ContractIdsDto {
        val contractIds: MutableList<Long> = mutableListOf()
        for (createContractDTO in createContractDTOs) {
            createContractDTO.run {
                templateRepository.findByIdAndDeletedFalse(templateId)?.let { template ->
                    template.let { it ->
                        var fileName = it.filePath.substringAfterLast("/")
                        val fileType = fileName.substringAfterLast(".")
                        fileName = fileName.substringBeforeLast(".")
                        fileName = fileName.substring(0, fileName.length - 36)
                        fileName = fileName + UUID.randomUUID() + "." + fileType
                        val contractFilePathDocx = "./files/contracts/${fileName}"
                        Files.copy(Paths.get(it.filePath), Paths.get(contractFilePathDocx))

                        changeAllKeysToValues(templateId, contractFilePathDocx, fields)
                        val contract = contractRepository.save(Contract(it, clientPassport, contractFilePathDocx))

                        val contractFieldValueMap: MutableList<ContractFieldValue> = mutableListOf()
                        for (fieldEntry in fields.entries) {
                            fieldRepository.findByName(fieldEntry.key)?.let {
                                contractFieldValueMap.add(ContractFieldValue(contract, it, fieldEntry.value))
                            }
                        }
                        contractFieldValueRepository.saveAll(contractFieldValueMap)

                        fileName = fileName.substringBeforeLast(".")
                        val contractFilePathPdf = "./files/contracts/${fileName}.pdf"
                        convertWordToPdf(
                            contractFilePathDocx,
                            contractFilePathPdf
                        )
                        contractIds.add(contract.id!!)
                    }
                } ?: throw TemplateNotFoundException()
            }
        }
        return ContractIdsDto(contractIds)
    }

    fun getContract(id: Long): ResponseEntity<Resource>? {
        return contractRepository.findByIdAndDeletedFalse(id)?.let {
            getResource(it.contractFilePath)

        }
    }

    private fun getResource(path: String): ResponseEntity<Resource> {
        val filePath = Paths.get("attaches/${path}").normalize()
        val resource: Resource?
        resource = UrlResource(filePath.toUri())
        if (!resource.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        var contentType = Files.probeContentType(filePath)
        if (contentType == null) {
            contentType = "application/octet-stream" // Fallback content type
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource)
    }

    fun getAllOperatorContracts(id: Long): List<ContractDto> {
        val contracts = mutableListOf<ContractDto>()
        val optional = userRepository.findById(id)
        if (optional.isEmpty) throw UserNotFoundException()
        contractRepository.findAllByCreatedBy(optional.get()).let {
            it.forEach { item ->
                contracts.add(ContractDto.toDTO(item))
            }
        }
        return contracts
    }

    fun getAllContracts(): List<ContractDto>? {
        val contracts = mutableListOf<ContractDto>()
        contractRepository.findAllNotDeleted().let {
            it.forEach { item ->
                contracts.add(ContractDto.toDTO(item))
            }
        }
        return contracts
    }

    private fun convertWordToPdf(inputStream: InputStream, outputStream: OutputStream) {
//        val wordMLPackage = WordprocessingMLPackage.load(inputStream)
//        Docx4J.toPDF(wordMLPackage, outputStream)


        Paths.get("asd").absolute().name
        XWPFDocument(inputStream).use { wordDocument ->
            PdfWriter(outputStream).use { pdfWriter ->
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                // Extract paragraphs and write them to the PDF
                for (paragraph in wordDocument.paragraphs) {
                    document.add(Paragraph(paragraph.text))
                }

                document.close()
            }
        }
    }

    private fun convertWordToPdf(inputFile: String, outputFileDir: String) {
        val processBuilder = ProcessBuilder(
            "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
            "--headless",
            "--convert-to", "pdf",
            "--outdir", outputFileDir,
            inputFile
        )
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorMessage = process.errorStream.bufferedReader().readText()
            println("Error during conversion: $errorMessage")
        }
    }

    private fun getKeys(filePath: String): MutableList<String> {
        val document = readDocFile(filePath)
        val keys = mutableSetOf<String>()
        for (table in document.tables)
            keys.addAll(getKeys(table))
        keys.addAll(getKeys(document.paragraphs))
        return keys.toMutableList()
    }

    private fun getKeys(table: XWPFTable): MutableList<String> {
        val keys = mutableListOf<String>()
        for (row in table.rows)
            for (tableCell in row.tableCells)
                keys.addAll(getKeys(tableCell.paragraphs))
        return keys
    }

    private fun getKeys(paragraphs: List<XWPFParagraph>): MutableList<String> {
        val keys = mutableListOf<String>()
        for (paragraph in paragraphs)
            keys.addAll(getKeys(paragraph))
        return keys
    }

    private fun getKeys(paragraph: XWPFParagraph): MutableList<String> {
        val keys: MutableList<String> = mutableListOf()
        paragraph.run {
            while (true) {
                val firstIndex = text.indexOf("##")
                if (firstIndex == -1) break

                val lastIndex = text.indexOf("##", firstIndex + 2)
                if (lastIndex == -1) break

                keys.add(text.substring(firstIndex, lastIndex))
                break
            }
        }
        return keys
    }

    fun getContractsByClint(clientPassport: String): List<ContractDto> {
        return contractRepository.findByClientPassportAndDeletedFalse(clientPassport).map { ContractDto.toDTO(it) }
    }

    fun upDateTemplate(id: Long, file: MultipartFile){
        val template = templateRepository.findByIdAndDeletedFalse(id) ?: throw TemplateNotFoundException()
        val filename = file.originalFilename!!.substringBeforeLast(".") + "-update-file-" + UUID.randomUUID() + ".docx"
        val filePath = "./files/templates/$filename"
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, Paths.get(filePath))
        }
        val keys = getKeys(filePath)
        val fields = getFieldsByKeys(keys)

        template.let {
            it.fields= fields.toMutableList()
            it.filePath=filePath
        }
        templateRepository.save(template)
    }
}

@Service
class FieldServiceImpl(
    private val fieldRepository: FieldRepository,
    private val templateRepository: TemplateRepository,
) : FieldService {
    override fun createField(dto: FieldDTO) {
        dto.run {
            if (fieldRepository.existsByName(name)) throw ExistsFieldException()
            fieldRepository.saveAndRefresh(FieldDTO.toEntity(name, type))
        }
    }

    override fun getFieldById(id: Long): FieldGetDto {
        return fieldRepository.findByIdAndDeletedFalse(id)?.let { FieldGetDto.toDTO(it) }
            ?: throw FieldNotFoundException()
    }

    override fun getAllField(): List<FieldGetDto> {
        return fieldRepository.findAllNotDeleted().map { FieldGetDto.toDTO(it) }
    }

    override fun updateField(id: Long, updateDto: FieldUpdateDTO) {
        val field = fieldRepository.findByIdAndDeletedFalse(id) ?: throw FieldNotFoundException()
        val template =
            templateRepository.findByIdAndDeletedFalse(updateDto.templateId) ?: throw TemplateNotFoundException()
        val fields = template.fields

        if (!fields.contains(field)) {
            throw FieldNotBelongToTemplate()
        }


        val newField: Field
        updateDto.run {
            if (fieldRepository.existsByName(name!!)) throw ExistsFieldException()
            newField = Field(name, TypeEnum.valueOf(type!!.uppercase()))
        }
        val saveField = fieldRepository.save(newField)
        val index = fields.indexOf(field)
        fields[index] = saveField
        templateRepository.save(template)

    }

    override fun deleteField(id: Long) {
        fieldRepository.trash(id) ?: FieldNotFoundException()
    }
}