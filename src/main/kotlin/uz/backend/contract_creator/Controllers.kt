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

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun update(@PathVariable id: Long, @RequestBody fieldUpdateRequest: FieldUpdateRequest) =
        service.updateField(id, fieldUpdateRequest)

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun delete(@PathVariable id: Long) = service.deleteField(id)

    @GetMapping("{id}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun get(@PathVariable id: Long) = service.getFieldById(id)

    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getAll(pageable: Pageable) = service.getAllField(pageable)
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

    @PostMapping
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun addTemplate(@RequestParam("file") file: MultipartFile, @RequestParam name: String) =
        docFileService.createNewTemplate(file, name)

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun update(@RequestParam("file") file: MultipartFile, @PathVariable id: Long) =
        docFileService.upDateTemplate(id, file)

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun delete(@PathVariable("id") id: Long) = docFileService.deleteTemplate(id)

    @GetMapping("{id}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun get(@PathVariable("id") id: Long) = docFileService.getKeysByTemplateId(id)

    @GetMapping("show/{id}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun show(@PathVariable("id") id: Long) = docFileService.show(id)


    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun getAll(pageable: Pageable) = docFileService.getAllTemplates(pageable)
}

@RestController
@RequestMapping("/contract")
class ContractController(
    private val docFileService: DocFileService,
    private val fileService: FieldServiceImpl
) {

    @PostMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun addContract(@RequestBody contractDTOs: AddContractRequest) = docFileService.addContract(contractDTOs)

    @PutMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun updateContract(@RequestBody updateContractRequest: UpdateContractRequest) =
        docFileService.updateContract(updateContractRequest)

    @DeleteMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun deleteContract(@PathVariable("id") id: Long) = docFileService.deleteContract(id)

    @PostMapping("/generate")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun generateContract(@RequestBody generateContractDTO: GenerateContractDTO) =
        fileService.generateContract(generateContractDTO)

    @GetMapping("/download/{hashCode}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun downloadContract(@PathVariable hashCode: String) = docFileService.downloadContract(hashCode)

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun get(@PathVariable("id") id: Long) = docFileService.getContract(id)

    @GetMapping("/get-by-operator-id")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun getContractsByOperatorId() = docFileService.getAllOperatorContracts(getUserId()!!)

    @GetMapping("get-job/{id}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_OPERATOR.name())"
    )
    fun getOneJob(@PathVariable id: Long) = docFileService.getOneJob(id)

    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getAll() = docFileService.getAllContracts()

    @GetMapping("get-jobs")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getJobs() = docFileService.getJobs()
}

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/me")
    fun getMe() = userService.getMe()


    @PutMapping("change-role/{userId}")
    @PreAuthorize("hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name())")
    fun changeRole(@PathVariable userId: Long, @RequestParam role: RoleEnum) = userService.changeRole(userId, role)

    @PutMapping("give-permission")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun givePermission(
        @RequestParam userId: Long,
        @RequestParam contractId: Long
    ) = userService.givePermission(userId, contractId)

    @GetMapping("{userId}")
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getOneUser(@PathVariable userId: Long) = userService.getOneUser(userId)

    @GetMapping
    @PreAuthorize(
        "hasAnyRole(T(uz.backend.contract_creator.RoleEnum).ROLE_ADMIN.name()," +
                "T(uz.backend.contract_creator.RoleEnum).ROLE_DIRECTOR.name())"
    )
    fun getAll(@RequestParam search: String?, @RequestParam userStatus: UserStatus?, pageable: Pageable) =
        userService.getAll(search,userStatus,pageable)
}