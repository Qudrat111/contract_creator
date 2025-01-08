package uz.backend.contract_creator

import jakarta.validation.Valid
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(BaseExceptionHandler::class)
    fun handleAccountException(exception: BaseExceptionHandler): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
}


@RestController
@RequestMapping("/field")
class FieldController(
    private val service: FieldService,
) {
    @PostMapping()
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun create(@RequestBody @Valid fieldDTO: FieldDTO) = service.createField(fieldDTO)

    @GetMapping("{id}")
    fun get(@PathVariable id: Long) = service.getFieldById(id)

    @GetMapping
    fun getAll() = service.getAllField()

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun update(@PathVariable id: Long, @RequestBody fieldUpdateDTO: FieldUpdateDTO) = service.updateField(id, fieldUpdateDTO)

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
    fun addTemplate(@RequestParam("file") file: MultipartFile, @RequestParam name: String) =
        docFileService.createNewTemplate(file, name)

    @GetMapping("/{id}")
    fun get(@PathVariable("id") id: Long) = docFileService.getKeysByTemplateId(id)

    @GetMapping("/show/{id}")
    fun show(@PathVariable("id") id: Long) = docFileService.getOneTemplate(id)

    @DeleteMapping("/id")
    fun delete(@PathVariable("id") id: Long) = docFileService.deleteTemplate(id)

    @GetMapping("/all")
    fun getAll() = docFileService.getAllTemplates()
}

@RestController
@RequestMapping("/contract")
class ContractController(
    private val docFileService: DocFileService,
) {

    @GetMapping("/add")
    fun addContract(@RequestBody contractDto: AddContractDTO) = docFileService.addContract(contractDto)

    @PostMapping("/download")
    fun downloadContract(@RequestBody downlaodDto: DownloadContractDTO) = docFileService.downloadContract(downlaodDto)
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
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
            "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())")
    fun getAll()=userService.getAllUsers()

    @GetMapping("{userId}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
            "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())")
    fun getOneUser(@PathVariable userId: Long) = userService.getOneUser(userId)

    @PutMapping("give-permission")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun givePermission(@RequestParam userId: Long,
                       @RequestParam contractId: Long) = userService.givePermission(userId,contractId)
}
