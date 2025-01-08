package uz.backend.contract_creator

import jakarta.validation.Valid
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Private

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(BaseExceptionHandler::class)
    fun handleAccountException(exception: BaseExceptionHandler): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
}

@RestController
@RequestMapping("/attach")
class AttachController(
    private val service: AttachService,
) {

    @PostMapping("upload")
    fun fileUpload(@RequestParam("file") file: MultipartFile) = service.saveToSystem(file)
}

@RestController
@RequestMapping("/field")
class FieldController(
    private val service: FieldService,
){
    @PostMapping()
    fun create(@RequestBody @Valid fieldDTO: FieldDTO) = service.createField(fieldDTO)

    @GetMapping("{id}")
    fun get(@PathVariable id: Long) = service.getFieldById(id)

    @GetMapping
    fun getAll() =service.getAllField()

    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody fieldUpdateDTO: FieldUpdateDTO) = service.updateField(id, fieldUpdateDTO)

}