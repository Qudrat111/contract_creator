package uz.backend.contract_creator

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: Long? = null,
    @LastModifiedBy var lastModifiedBy: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity(name = "users")
@Table(name = "users")
class User(
    @Column(length = 32, nullable = false) val firstName: String,
    @Column(length = 32, nullable = false) val lastName: String,
    @Column(length = 32, nullable = false, unique = true) val userName: String,
    @Column(length = 20, nullable = false) val passWord: String,
    @Enumerated(EnumType.STRING) @Column(length = 20) var role: RoleEnum

) : BaseEntity(), UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority(role.toString()))
    }

    @Override
    override fun getPassword(): String {
        return passWord
    }

    @Override
    override fun getUsername(): String {
        return userName
    }

}

@Entity
class Template(
    @Column(length = 50, nullable = false) val name: String,
    @Column(nullable = false) var filePath: String,
    @ManyToMany var fields: MutableList<Field>,
) : BaseEntity() {
    fun toResponseDto(): TemplateResponseDto {
        return TemplateResponseDto(id!!, name, fields.map { it.toResponseDto() }.toMutableList())
    }
}

@Entity
class Field(
    @Column(length = 45, nullable = false, unique = true) var name: String,
    @Enumerated(EnumType.STRING)@Column(length = 20) var type: TypeEnum
) : BaseEntity() {
    fun toResponseDto(): FieldResponseDto {
        return FieldResponseDto(id!!, name, type)
    }
}

@Entity(name = "contracts")
class Contract(
    @ManyToOne val template: Template,
    var contractFilePath: String?,
    @OneToMany(mappedBy = "contract") val allowedOperators: List<ContractAllowedUser> = mutableListOf()
) : BaseEntity()

@Entity
class ContractFieldValue(

    @ManyToOne val contract: Contract,
    @ManyToOne val field: Field,
    @Column(nullable = false) val value: String,
) : BaseEntity()

@Entity
class ContractAllowedUser(

    @ManyToOne @JoinColumn(nullable = false) val operator: User,
    @ManyToOne @JoinColumn(nullable = false) val contract: Contract

) : BaseEntity()

@Entity
class Job(
    val status : TaskStatusEnum,
    val fileType: FileTypeEnum,
    val zipFilePath: String,
    @ManyToMany val contracts: MutableList<Contract> = mutableListOf(),
): BaseEntity()
