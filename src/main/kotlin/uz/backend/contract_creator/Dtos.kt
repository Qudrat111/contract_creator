package uz.backend.contract_creator

import jakarta.validation.constraints.NotNull

data class BaseMessage(val code: Int, val message: String?)

data class TokenDTO(
    private val token: String
)

data class LogInDTO(
    @NotNull val username: String,

    @NotNull val password: String
)

data class SignInDTO(
    @NotNull val username: String,
    @NotNull var password: String,
    @NotNull val firstName: String,
    @NotNull val lastName: String,
) {
    fun toEntity(): User {
        return User(firstName, lastName, username, password, RoleEnum.ROLE_DEFAULT)
    }
}

data class UserDTO(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: RoleEnum,
){
    companion object {
        fun toResponse(user: User): UserDTO {
            user.run {
                return UserDTO(id!!, firstName, lastName, username, role)
            }
        }
    }
}

data class FieldDTO(
    @NotNull var name: String,
    @NotNull var type: String
) {
    companion object {
        fun toEntity(name: String, type: String): Field {
            return Field(name, TypeEnum.valueOf(type.uppercase()))
        }

        fun toDTO(it: Field): FieldDTO {
            return FieldDTO(it.name, it.type.name)
        }
    }
}

data class FieldUpdateDTO(
    val name: String?,
    val type: String?
)
data class DownloadContractDTO (
    @NotNull val contractId: Long,
    @NotNull val fileType: String
)
data class CreateContractDTO (
    @NotNull val templateId: Long,
    @NotNull val fields: Map<String, String>,
    @NotNull val clientPassport: String
)