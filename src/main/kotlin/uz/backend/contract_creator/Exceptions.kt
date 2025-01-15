package uz.backend.contract_creator

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(BaseExceptionHandler::class)
    fun handleAccountException(ex: BaseExceptionHandler): BaseMessage {
        return ex.getErrorMessage(errorMessageSource)
    }


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<BaseMessage> {
        val errors = ex.bindingResult.fieldErrors.joinToString("\n") {
            "(${it.field}) ${it.defaultMessage} (${
                errorMessageSource.getMessage(
                    "REJECTED_VALUE", arrayOf(),
                    LocaleContextHolder.getLocale()
                )
            }: ${it.rejectedValue})"
        }
        return ResponseEntity.badRequest().body(BaseMessage(400, errors))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneralException(ex: Exception): BaseMessage {
        return BaseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message ?: "An error occurred")
    }
}


sealed class BaseExceptionHandler : RuntimeException() {

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

class UserNotFoundException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.USER_NOT_FOUND
}

class BadCredentialsException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.BAD_CREDENTIALS
}

class FieldNotFoundException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.FIELD_NOT_FOUND
}

class ExistsFieldException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.EXISTS_FIELD
}

class FileNotFoundException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.FILE_NOT_FOUND
}

class TemplateNotFoundException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.TEMPLATE_NOT_FOUND
}

class FieldNotBelongToTemplate : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.FILED_NOT_BELONG_TEMPLATE
}

class ContractNotFoundException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.CONTRACT_NOT_FOUND

}

class AccessDeniedException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.ACCESS_DENIED
}

class InvalidFileTypeException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.INVALID_FILE_TYPE
}

class UsernameAlreadyExists : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.USERNAME_ALREADY_EXISTS

}

class JobNotFoundException : BaseExceptionHandler() {
    override fun errorCode() = ErrorCodes.JOB_NOT_FOUND

}