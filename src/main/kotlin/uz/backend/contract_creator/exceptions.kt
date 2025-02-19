package uz.backend.contract_creator

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource

sealed class BaseExceptionHandler : RuntimeException(){

    abstract fun errorCode(): ErrorCodes

    open fun getAllArguments(): Array<Any?>? = null

    fun getErrorMessage(resourceBundle: ResourceBundleMessageSource): BaseMessage {

        val message = try {
            resourceBundle.getMessage(
                errorCode().name, getAllArguments(), LocaleContextHolder.getLocale()
            )
        } catch (e: Exception) {
            e.message
        }
        return BaseMessage(errorCode().code, message)

    }

}

class UserNotFoundException():BaseExceptionHandler(){
    override fun errorCode() = ErrorCodes.USER_NOT_FOUND
}

class BadCredentialsException():BaseExceptionHandler(){
    override fun errorCode() = ErrorCodes.BAD_CREDENTIALS
}