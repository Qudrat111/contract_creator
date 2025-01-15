package uz.backend.contract_creator

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): List<T> = findAll(isNotDeletedSpecification, pageable).content
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
        findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }

    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

@Repository
interface UserRepository : BaseRepository<User> {
    fun existsByUserName(username: String): Boolean
    fun findByUserNameAndDeletedFalse(username: String): User?
}

interface TemplateRepository : BaseRepository<Template> {
    fun existsByIdAndDeletedFalse(templateId: Long): Boolean
}

interface FieldRepository : BaseRepository<Field> {
    fun existsByName(name: String): Boolean
    fun findByNameAndDeletedFalse(name:String): Field?
}

@Repository
interface ContractRepository : BaseRepository<Contract> {
    fun findAllByCreatedBy(createdBy: Long): List<Contract>

}

@Repository
interface ContractFieldValueRepository : BaseRepository<ContractFieldValue> {
    fun findAllByContractId(contractId: Long):List<ContractFieldValue>
    @Query("""
        select cfv from contractFieldValue  cfv
        where cfv.contract.id = :contractId and cfv.field.id = :fieldId
    """)
    fun findContractFieldValue(contractId: Long,fieldId:Long):ContractFieldValue
}

@Repository
interface ContactAllowedUserRepository : BaseRepository<ContractAllowedUser>

@Repository
interface JobRepository : BaseRepository<Job> {
    fun findByHashCodeAndDeletedFalse(hashCode: String): Job?
    fun findAllByCreatedByAndDeletedFalseOrderByIdDesc(createdBy: Long): List<Job>
    fun findByIdAndCreatedByAndDeletedFalse(id: Long, createdBy: Long): Job?
}
