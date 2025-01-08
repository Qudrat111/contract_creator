package uz.backend.contract_creator

import jakarta.validation.constraints.NotNull
data class BaseMessage(val code: Int, val message: String?)

data class TokenDTO (
    private val token: String
)

data class LogInDTO (
    @NotNull val username:  String,

    @NotNull val password: String
)

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