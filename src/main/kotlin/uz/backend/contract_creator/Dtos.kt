package uz.backend.contract_creator

import jakarta.validation.constraints.NotNull

data class BaseMessage(val code: Int, val message: String?)

data class LoginRequest(
    val username: String,
    val password: String,
)

data class SignUpRequest(
    val username: String,
    var password: String,
    val firstName: String,
    val lastName: String,
) {
    fun toEntity(): User {
        return User(firstName, lastName, username, password, RoleEnum.ROLE_DEFAULT)
    }
}

data class UserResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: RoleEnum,
    val status: UserStatus?
) {
    companion object {
        fun toResponse(user: User): UserResponse {
            user.run {
                return UserResponse(id!!, firstName, lastName, username, role, status)
            }
        }
    }
}

data class FieldResponse(
    val id: Long,
    var name: String,
    var type: String,
) {
    companion object {
        fun toDTO(it: Field): FieldResponse {
            return FieldResponse(it.id!!, it.name, it.type.name)
        }
    }
}

data class FieldRequest(
    var name: String,
    var type: String,
) {
    companion object {
        fun toEntity(name: String, type: String): Field {
            return Field(name, TypeEnum.valueOf(type.uppercase()))
        }

        fun toDTO(it: Field): FieldRequest {
            return FieldRequest(it.name, it.type.name)
        }
    }
}

data class FieldUpdateRequest(
    val templateId: Long,
    val name: String?,
    val type: String?,
)

data class GenerateContractDTO(
    @NotNull val contractIds: List<Long>,
    @NotNull val fileType: String,
)

data class AddContractRequest(
    @NotNull val templateId: Long,
    @NotNull val contractFieldValues: List<ContractCreateDTO>,
)

data class ContractResponse(
    val contractId: Long,
    val contract: List<ContractCreateDTO>,
) {
    companion object {
        fun toResponse(contractId: Long, contractFieldValues: List<ContractCreateDTO>) =
            ContractResponse(contractId, contractFieldValues)
    }
}

data class ContractCreateDTO(
    val fieldName: String,
    val value: String,
) {
    companion object {
        fun toResponse(contractFiledValue: ContractFieldValue) =
            ContractCreateDTO(
                contractFiledValue.field.name,
                contractFiledValue.value
            )
    }
}


data class UpdateContractRequest(
    val contractId: Long,
    val contactFieldValues: List<ContractCreateDTO>,
)

data class TemplateResponse(
    val id: Long?,
    val name: String,
    val keys: List<FieldRequest>,
) {
    companion object {
        fun toResponse(template: Template): TemplateResponse {
            val fieldDTos = mutableListOf<FieldRequest>()
            template.fields.forEach { fieldDTO ->
                fieldDTos.add(FieldRequest.toDTO(fieldDTO))
            }
            return TemplateResponse(template.id, template.name, fieldDTos)
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
) {
    companion object {
        fun toResponseDto(save: Template): TemplateResponseDto {
            save.run {
                return TemplateResponseDto(
                    id!!,
                    name,
                    fields.map { FieldResponseDto.toResponseDto(it) }.toMutableList()
                )
            }
        }
    }
}

data class FieldResponseDto(
    var id: Long,
    var name: String,
    var type: TypeEnum,
) {
    companion object {
        fun toResponseDto(it: Field): FieldResponseDto {
            it.run {
                return FieldResponseDto(id!!, name, type)
            }
        }
    }
}


class TokenDTO(
    val token: String,
    val userResponse: UserResponse,
)

data class JobResponseDTO(
    val id: Long,
    val fileType: FileTypeEnum,
    val status: TaskStatusEnum,
    var hashCode: String? = null,
) {
    companion object {

        fun toResponseDTO(it: Job): JobResponseDTO {
            it.run {
                return JobResponseDTO(id!!, fileType, status)
            }
        }

        fun toResponseDTOWithHashCode(it: Job): JobResponseDTO {
            it.run {
                return JobResponseDTO(id!!, fileType, status, hashCode)
            }
        }
    }
}

data class GetOneTemplateKeysDTO(
    val data: List<GetFieldDto>,
)

data class GetFieldDto(
    val key: String,
    val required: Boolean,
)

