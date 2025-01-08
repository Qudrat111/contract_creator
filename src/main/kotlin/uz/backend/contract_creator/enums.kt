package uz.backend.contract_creator

import org.springframework.security.core.authority.SimpleGrantedAuthority

enum class RoleEnum {
    ROLE_DIRECTOR,
    ROLE_ADMIN,
    ROLE_OPERATOR,
    ROLE_DEFAULT,
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
    FILE_NOT_FOUND(300)
}