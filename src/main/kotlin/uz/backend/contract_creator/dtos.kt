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
data class DownloadContractDTO (
    @NotNull val contractId: Long,
    @NotNull val fileType: String
)
data class AddContractDTO (
    @NotNull val templateId: Long,
    @NotNull val fields: Map<String, String>,
    @NotNull val clientPassport: String
)