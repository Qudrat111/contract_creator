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
    @Column(nullable = false)val firstName: String,
    @Column(nullable = false)val lastName: String,
    @Column(nullable = false, unique = true)val username: String,
    @Column(nullable = false)val password: String,
    @Enumerated(EnumType.STRING) val role: RoleEnum

): BaseEntity(), UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority(role.toString()))
    }

    override fun getPassword(): String {
       return password
    }

    override fun getUsername(): String {
        return username
    }

}

@Entity
class Template(
    @Column(nullable = false) val name: String,
    @Column(nullable = false)val filePath: String,
    @ManyToMany val fields: List<Field>,
): BaseEntity()

@Entity
class Field(
    @Column(nullable = false)val name: String,
    @Enumerated(EnumType.STRING)val type: TypeEnum
): BaseEntity()

@Entity
class Contract(

    @ManyToOne val template : Template? = null,
    @Column(nullable = false) val clientPassport: String,
    @Column(nullable = false) val contractFilePath: String,

): BaseEntity()

@Entity
class ContractFieldValue(

    @ManyToOne val contract: Contract,
    @OneToOne val field: Field,
    @Column(nullable = false) val value: String,

): BaseEntity()
