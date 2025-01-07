package uz.backend.contract_creator

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sun.security.jgss.GSSUtil.login
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

interface AuthService : UserDetailsService {
    fun logIn(signInDTO: LogInDTO): TokenDTO?

}

@Service
class AuthServiceImpl(
    private val authenticationProvider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) : AuthService {
    override fun logIn(signInDTO: LogInDTO): TokenDTO? {
        val authentication =
            UsernamePasswordAuthenticationToken(signInDTO.username, signInDTO.password)

        authenticationProvider.authenticate(authentication)

        val user  = loadUserByUsername(signInDTO.username)

        val matches: Boolean = passwordEncoder.matches(signInDTO.password, user.password)

        if (!matches) throw UserNotFoundException()

        val token: String = jwtProvider.generateToken(signInDTO.username)

        return TokenDTO(token)
    }

    override fun loadUserByUsername(username: String): UserDetails {

       return userRepository.findByUsername(username) ?: throw RuntimeException("User $username not found")
    }
}

@Service
class AttachService {
    fun saveToSystem(file: MultipartFile): String? {
        val folder = File("Files")
        if (!folder.exists()) folder.mkdir()

        val bytes = file.bytes
        val path: Path = Paths.get("Files/" + file.originalFilename)
        Files.write(path, bytes)
        return file.originalFilename
    }
}