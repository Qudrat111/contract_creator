package uz.backend.contract_creator

enum class RoleEnum {
    ROLE_DIRECTOR,
    ROLE_ADMIN,
    ROLE_OPERATOR,
    ROLE_NEW,
}

enum class TypeEnum{
    STRING,
    DATE,
    NUMBER,
}

enum class ErrorCodes(val code: Int){
    USER_NOT_FOUND(100),
    BAD_CREDENTIALS(101),
    FIELD_NOT_FOUND(102),
    EXISTS_FIELD(103),
    CONTRACT_NOT_FOUND(104)
}