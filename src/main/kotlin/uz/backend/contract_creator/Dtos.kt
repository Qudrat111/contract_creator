package uz.backend.contract_creator

import jakarta.validation.constraints.NotNull

data class BaseMessage(val code: Int, val message: String?)

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
) {
    companion object {
        fun toResponse(user: User): UserDTO {
            user.run {
                return UserDTO(id!!, firstName, lastName, username, role)
            }
        }
    }
}

data class FieldGetDto(
    val id: Long,
    var name: String,
    var type: String,
) {
    companion object {
        fun toDTO(it: Field): FieldGetDto {
            return FieldGetDto(it.id!!, it.name, it.type.name)
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
    val templateId: Long,
    val name: String?,
    val type: String?
)

data class GenerateContractDTO(
    @NotNull val contractIds: List<Long>,
    @NotNull val fileType: String
)

data class AddContractDTO(
    @NotNull val contract: List<ContractDTO>,
) {
    companion object {
        fun toResponse(contractFiledValue: ContractFieldValue) =
            ContractFieldValueDto(
                contractFiledValue.contract.id!!,
                contractFiledValue.field.name,
                contractFiledValue.value
            )
    }
}

data class ContractDTO(
    val templateId: Long,
    val fieldName: String,
    val value: String
)

data class ContractFieldValueDto(
    val contractId: Long,
    val fieldName: String,
    val value: String
)

data class UpdateContractDTO(
    @NotNull val contactFieldValues: List<ContractFieldValueDto>
) {
    companion object {
        fun toResponse(contractFiledValue: ContractFieldValue) = AddContractDTO.toResponse(contractFiledValue)
    }
}

data class TemplateDto(
    val id: Long?,
    @NotNull val name: String,
    @NotNull val keys: List<FieldDTO>
) {
    companion object {
        fun toResponse(template: Template): TemplateDto {
            val fieldDTos = mutableListOf<FieldDTO>()
            template.fields.forEach { fieldDTO ->
                fieldDTos.add(FieldDTO.toDTO(fieldDTO))
            }
            return TemplateDto(template.id, template.name, fieldDTos)
        }
    }
}

data class ContractDto(
    val contractId: Long,
    val templateName: String,
) {
    companion object {
        fun toDTO(it: Contract): ContractDto {
            return ContractDto(it.id!!, it.template.name)
        }
    }
}

data class TemplateResponseDto(
    val id: Long,
    val name: String,
    val fields: MutableList<FieldResponseDto>,
)

data class FieldResponseDto(
    var id: Long,
    var name: String,
    var type: TypeEnum
)

data class ContractIdsDto(
    val contractIds: MutableList<Long> = mutableListOf()
)

class TokenDTO(
    val token: String,
    val userDTO: UserDTO
)