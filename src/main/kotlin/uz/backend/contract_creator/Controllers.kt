package uz.backend.contract_creator

import jakarta.validation.Valid
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(BaseExceptionHandler::class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleAccountException(ex: BaseExceptionHandler): BaseMessage {
        return ex.getErrorMessage(errorMessageSource)
    }


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<BaseMessage> {
        val errors = exception.bindingResult.fieldErrors.joinToString("\n") {
            "(${it.field}) ${it.defaultMessage} (${errorMessageSource.getMessage("REJECTED_VALUE", arrayOf(),
                LocaleContextHolder.getLocale())}: ${it.rejectedValue})"
        }
        return ResponseEntity.badRequest().body(BaseMessage(400, errors))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneralException(ex: Exception): BaseMessage {
        return BaseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message ?: "An error occurred")
    }
}


@RestController
@RequestMapping("/field")
class FieldController(
    private val service: FieldService,
) {
    @PostMapping()
    fun create(@RequestBody @Valid fieldDTO: FieldDTO) = service.createField(fieldDTO)

    @GetMapping("{id}")
    fun get(@PathVariable id: Long) = service.getFieldById(id)

    @GetMapping
    fun getAll() = service.getAllField()

    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody fieldUpdateDTO: FieldUpdateDTO) =
        service.updateField(id, fieldUpdateDTO)

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
@RequestMapping("/user")
class UserController(
    private val userService: UserService
) {
    @PutMapping("change-role/{userId}")
    fun changeRole(@PathVariable userId: Long, @RequestParam role: RoleEnum) = userService

    @GetMapping()
    fun getAll() = userService.getAllUsers()

    @GetMapping("{userId}")
    fun getOneUser(@PathVariable userId: Long) = userService.getOneUser(userId)

    @PutMapping("give-permission")
    fun givePermission(
        @RequestParam userId: Long,
        @RequestParam contractId: Long
    ) = userService.givePermission(userId, contractId)
}

@RestController
@RequestMapping("/template")
class TemplateController(private val docFileService: DocFileService) {

    @PostMapping("add-template")
    fun addTemplate(@RequestParam("file") file: MultipartFile, @RequestParam name: String) =
        docFileService.createNewTemplate(file, name)

    @GetMapping("/{id}")
    fun get(@PathVariable("id") id: Long) = docFileService.getKeysByTemplateId(id)

    @GetMapping("/show/{id}")
    fun show(@PathVariable("id") id: Long) = docFileService.getOneTemplate(id)

    @DeleteMapping("/id")
    fun delete(@PathVariable("id") id: Long) = docFileService.deleteTemplate(id)

//    @GetMapping("/all")
//    fun getAll() = docFileService.getAllTemplates()
}

@RestController
@RequestMapping("/contract")
class ContractController(
    private val docFileService: DocFileService,
) {

    @GetMapping("/add")
    fun addContract(@RequestBody contractDto: CreateContractDTO) = docFileService.addContract(contractDto)

    @PostMapping("/download")
    fun downloadContract(@RequestBody downloadDto: DownloadContractDTO) = docFileService.downloadContract(downloadDto)
}