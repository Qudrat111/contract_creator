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

@Entity()
@Table(name = "users")
class User(
    @Column(nullable = false) val firstName: String,
    @Column(nullable = false) val lastName: String,
    @Column(nullable = false, unique = true) val userName: String,
    @Column(nullable = false) val passWord: String,
    @Enumerated(EnumType.STRING) var role: RoleEnum

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
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val filePath: String,
    @ManyToMany val fields: List<Field>,
) : BaseEntity()

@Entity
class Field(
    @Column(nullable = false, unique = true) var name: String,
    @Enumerated(EnumType.STRING) var type: TypeEnum
) : BaseEntity()

@Entity(name = "contracts")
class Contract(
    @ManyToOne val template: Template? = null,
    @Column(nullable = false) val clientPassport: String,
    @Column(nullable = false) val contractFilePath: String,
    ) : BaseEntity()

@Entity
class ContractFieldValue(

    @ManyToOne val contract: Contract,
    @OneToOne val field: Field,
    @Column(nullable = false) val value: String,
    ) : BaseEntity()

@Entity
class ContractAllowedUser(

    @ManyToOne @JoinColumn(nullable = false) val operator: User,
    @ManyToOne @JoinColumn(nullable = false) val contract: Contract

): BaseEntity()
