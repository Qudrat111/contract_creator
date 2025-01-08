package uz.backend.contract_creator

import org.springframework.security.core.context.SecurityContextHolder

fun getUserId(): Long? {
    val authentication = SecurityContextHolder.getContext().authentication

    if (authentication != null && authentication.isAuthenticated) {
        val principal = authentication.principal

        if (principal is User) {
            return principal.id
        }
    }
    return null
}