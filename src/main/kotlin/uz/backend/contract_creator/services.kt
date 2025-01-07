package uz.backend.contract_creator

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

interface AuthService : UserDetailsService {

    fun logIn(signInDTO: LogInDTO): TokenDTO
    fun signIn(signInDTO: SignInDTO): UserDTO

}

@Service
class AuthServiceImpl(
    private val authenticationProvider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) : AuthService {
    override fun logIn(signInDTO: LogInDTO): TokenDTO {
        val authentication =
            UsernamePasswordAuthenticationToken(signInDTO.username, signInDTO.password)

        authenticationProvider.authenticate(authentication)

        val user  = loadUserByUsername(signInDTO.username)

        val matches: Boolean = passwordEncoder.matches(signInDTO.password, user.password)

        if (!matches) throw UserNotFoundException()

        val token: String = jwtProvider.generateToken(signInDTO.username)

        return TokenDTO(token)
    }

    override fun signIn(signInDTO: SignInDTO): UserDTO {
        return signInDTO.run {
            UserDTO.toResponse(userRepository.save(this.toEntity()))
        }
    }

    override fun loadUserByUsername(username: String): UserDetails {

       return userRepository.findByUsername(username) ?: throw RuntimeException("User $username not found")
    }
}