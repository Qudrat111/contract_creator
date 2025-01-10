package uz.backend.contract_creator

import jakarta.validation.Valid
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile

@RestControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(BaseExceptionHandler::class)
    fun handleAccountException(ex: BaseExceptionHandler): BaseMessage {
        println("asd1 $ex")
        return ex.getErrorMessage(errorMessageSource)
    }


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<BaseMessage> {
        println("asd2 $ex")
        val errors = ex.bindingResult.fieldErrors.joinToString("\n") {
            "(${it.field}) ${it.defaultMessage} (${
                errorMessageSource.getMessage(
                    "REJECTED_VALUE", arrayOf(),
                    LocaleContextHolder.getLocale()
                )
            }: ${it.rejectedValue})"
        }
        return ResponseEntity.badRequest().body(BaseMessage(400, errors))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneralException(ex: Exception): BaseMessage {
        println("asd3 $ex")
        return BaseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message ?: "An error occurred")
    }
}


@RestController
@RequestMapping("/field")
class FieldController(
    private val service: FieldService,
) {
    @PostMapping
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun create(@RequestBody @Valid fieldDTO: FieldDTO) = service.createField(fieldDTO)

    @GetMapping("{id}")
    fun get(@PathVariable id: Long) = service.getFieldById(id)

    @GetMapping
    fun getAll() = service.getAllField()

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun update(@PathVariable id: Long, @RequestBody fieldUpdateDTO: FieldUpdateDTO) =
        service.updateField(id, fieldUpdateDTO)

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun delete(@PathVariable id: Long) = service.deleteField(id)

}

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("log-in")
    fun logIn(@RequestBody @Valid logInDTO: LogInDTO) = authService.logIn(logInDTO)

    @PostMapping("sign-in")
    fun signIn(@RequestBody @Valid signInDTO: SignInDTO) = authService.signIn(signInDTO)

}

@RestController
@RequestMapping("/template")
class TemplateController(private val docFileService: DocFileService) {

    @PostMapping("add-template")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun addTemplate(@RequestParam("file") file: MultipartFile, @RequestParam name: String) =
        docFileService.createNewTemplate(file, name)

    @GetMapping("/{id}")
    fun get(@PathVariable("id") id: Long) = docFileService.getKeysByTemplateId(id)

    @GetMapping("/show/{id}")
    fun show(@PathVariable("id") id: Long) = docFileService.getOneTemplate(id)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun delete(@PathVariable("id") id: Long) = docFileService.deleteTemplate(id)

    @GetMapping("/all")
    fun getAll() = docFileService.getAllTemplates()

    @PutMapping("update-template/{id}")
    fun update(@RequestParam("file") file: MultipartFile, @PathVariable id:Long) =docFileService.upDateTemplate(id, file)
}

@RestController
@RequestMapping("/contract")
class ContractController(
    private val docFileService: DocFileService,
) {
    @GetMapping("get-by-clint/{clientPassport}")
    fun getByClint(@PathVariable clientPassport: String) = docFileService.getContractsByClint(clientPassport)

    @GetMapping("/add")
    fun addContract(@RequestBody contractDTOs: List<AddContractDTO>) = docFileService.addContract(contractDTOs)

    @PostMapping("/download")
    fun downloadContract(@RequestBody downloadDto: DownloadContractDTO) = docFileService.downloadContract(downloadDto)

    @GetMapping("/{id}")
    fun get(@PathVariable("id") id: Long) = docFileService.getContract(id)

    @GetMapping("/get")
    fun getContractsById() = docFileService.getAllOperatorContracts(getUserId()!!)


    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getAll() = docFileService.getAllContracts()
}


@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService
) {
    @PutMapping("change-role/{userId}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun changeRole(@PathVariable userId: Long, @RequestParam role: RoleEnum) = userService

    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getAll() = userService.getAllUsers()

    @GetMapping("{userId}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getOneUser(@PathVariable userId: Long) = userService.getOneUser(userId)

    @PutMapping("give-permission")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun givePermission(
        @RequestParam userId: Long,
        @RequestParam contractId: Long
    ) = userService.givePermission(userId, contractId)
}