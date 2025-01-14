package uz.backend.contract_creator

enum class RoleEnum {
    ROLE_DIRECTOR,
    ROLE_ADMIN,
    ROLE_OPERATOR,
    ROLE_DEFAULT,
}

enum class TypeEnum {
    STRING,
    DATE,
    NUMBER,
}

enum class ErrorCodes(val code: Int) {
    USER_NOT_FOUND(100),
    BAD_CREDENTIALS(101),
    FIELD_NOT_FOUND(102),
    EXISTS_FIELD(103),
    FILE_NOT_FOUND(300),
    TEMPLATE_NOT_FOUND(104),
    FILED_NOT_BELONG_TEMPLATE(105),
    CONTRACT_NOT_FOUND(106),
    ACCESS_DENIED(107),
    INVALID_FILE_TYPE(108),
    USERNAME_ALREADY_EXISTS(109),
    JOB_NOT_FOUND(110)
}

enum class TaskStatusEnum {
    PENDING,
    FAILED,
    FINISHED
}

enum class FileTypeEnum{
    PDF,
    DOCX
}