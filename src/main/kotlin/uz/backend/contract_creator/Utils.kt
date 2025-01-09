package uz.backend.contract_creator

import org.springframework.security.core.context.SecurityContextHolder

fun getUserId() = SecurityContextHolder.getContext().getUserId()
