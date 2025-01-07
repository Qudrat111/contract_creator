package uz.backend.contract_creator

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

interface AuthService : UserDetailsService {

    fun logIn(signInDTO: LogInDTO): String
    fun signIn(signInDTO: SignInDTO): UserDTO

}

@Service
class AuthServiceImpl(
    private val authenticationProvider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) : AuthService {
    override fun logIn(signInDTO: LogInDTO): String {
        val authentication =
            UsernamePasswordAuthenticationToken(signInDTO.username, signInDTO.password)

        authenticationProvider.authenticate(authentication)

        val user  = loadUserByUsername(signInDTO.username)

//        val matches: Boolean = passwordEncoder.matches(signInDTO.password, user.password)
//
//        if (!matches) throw UserNotFoundException()

        val token: String = jwtProvider.generateToken(signInDTO.username)

        return token
    }

    override fun signIn(signInDTO: SignInDTO): UserDTO {
        return signInDTO.run {
            val encoded = passwordEncoder.encode(signInDTO.password)
            this.password = encoded
            UserDTO.toResponse(userRepository.save(this.toEntity()))
        }
    }

    override fun loadUserByUsername(username: String): UserDetails {

       return userRepository.findByUserName(username) ?: throw RuntimeException("User $username not found")
    }
}