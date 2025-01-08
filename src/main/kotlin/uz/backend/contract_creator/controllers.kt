package uz.backend.contract_creator

import jakarta.validation.Valid
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
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
){
    @PutMapping("change-role/{userId}")
    fun changeRole(@PathVariable userId: Long, @RequestParam role: RoleEnum) = userService

    @GetMapping()
    fun getAll()=userService.getAllUsers()

    @GetMapping("{userId}")
    fun getOneUser(@PathVariable userId: Long) = userService.getOneUser(userId)

    @PutMapping("give-permission")
    fun givePermission(@RequestParam userId: Long,
                       @RequestParam contractId: Long) = userService.givePermission(userId,contractId)
}

@RestController
@RequestMapping("/template")
class TemplateController(private val docFileService: DocFileService) {

    @PostMapping("add-template")
    fun addTemplate(@RequestParam("file") file: MultipartFile, @RequestParam name: String) =
        docFileService.createNewTemplate(file, name)
}