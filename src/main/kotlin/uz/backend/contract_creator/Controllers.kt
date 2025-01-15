package uz.backend.contract_creator

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile



@RestController
@RequestMapping("/field")
class FieldController(
    private val service: FieldService,
) {
    @PostMapping
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun create(@RequestBody @Valid fieldRequest: FieldRequest) = service.createField(fieldRequest)

    @GetMapping("{id}")
    fun get(@PathVariable id: Long) = service.getFieldById(id)

    @GetMapping
    fun getAll(pageable: Pageable) = service.getAllField(pageable)

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun update(@PathVariable id: Long, @RequestBody fieldUpdateRequest: FieldUpdateRequest) =
        service.updateField(id, fieldUpdateRequest)

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
    fun logIn(@RequestBody @Valid loginRequest: LoginRequest) = authService.logIn(loginRequest)

    @PostMapping("sign-up")
    fun signIn(@RequestBody @Valid signUpRequest: SignUpRequest) = authService.signIn(signUpRequest)

}

@RestController
@RequestMapping("/template")
class TemplateController(private val docFileService: DocFileService) {

    @PostMapping()
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun addTemplate(@RequestParam("file") file: MultipartFile, @RequestParam name: String) =
        docFileService.createNewTemplate(file, name)

    @GetMapping("{id}")
    fun get(@PathVariable("id") id: Long) = docFileService.getKeysByTemplateId(id)

    @GetMapping("show/{id}")
    fun show(@PathVariable("id") id: Long) = docFileService.getOneTemplate(id)

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun delete(@PathVariable("id") id: Long) = docFileService.deleteTemplate(id)

    @GetMapping()
    fun getAll(pageable: Pageable) = docFileService.getAllTemplates(pageable)

    @PutMapping("{id}")
    fun update(@RequestParam("file") file: MultipartFile, @PathVariable id: Long) =
        docFileService.upDateTemplate(id, file)
}

@RestController
@RequestMapping("/contract")
class ContractController(
    private val docFileService: DocFileService,
    private val fileService: FieldServiceImpl
) {
    @PostMapping("/generate")
    fun generateContract(@RequestBody generateContractDTO: GenerateContractDTO) =
        fileService.generateContract(generateContractDTO)

    @GetMapping("/download/{hashCode}")
    fun downloadContract(@PathVariable hashCode: String) = docFileService.downloadContract(hashCode)

    @GetMapping("/{id}")
    fun get(@PathVariable("id") id: Long) = docFileService.getContract(id)

    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    @PostMapping
    fun addContract(@RequestBody contractDTOs: AddContractRequest) = docFileService.addContract(contractDTOs)

    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    @PutMapping
    fun updateContract(@RequestBody updateContractRequest: UpdateContractRequest) =
        docFileService.updateContract(updateContractRequest)

    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    @DeleteMapping("/{id}")
    fun deleteContract(@PathVariable("id") id: Long) = docFileService.deleteContract(id)

    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )

    @GetMapping("/get-by-operator-id")
    fun getContractsByOperatorId() = docFileService.getAllOperatorContracts(getUserId()!!)

    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getAll() = docFileService.getAllContracts()

    @GetMapping("get-jobs")
    fun getJobs() = docFileService.getJobs()

    @GetMapping("get-job/{id}")
    fun getOneJob(@PathVariable id: Long) = docFileService.getOneJob(id)
}


@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService
) {
    @PutMapping("change-role/{userId}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun changeRole(@PathVariable userId: Long, @RequestParam role: RoleEnum) = userService.changeRole(userId, role)

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