package uz.backend.contract_creator

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtFilter(@Lazy jwtProvider: JwtProvider, @Lazy authService: AuthService) :
    OncePerRequestFilter() {
    private val jwtProvider: JwtProvider = jwtProvider

    private val authService: AuthService = authService

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        checkAuth(request, response)
        filterChain.doFilter(request, response)
    }

    @Throws(IOException::class)
    private fun checkAuth(request: HttpServletRequest, response: HttpServletResponse) {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (Objects.isNull(authHeader)) return

        if (!authHeader.startsWith("Bearer ")) return

        val token = authHeader.substring(7)

        var username: String? = null

        try {
            username = jwtProvider.getSubject(token)
        } catch (e: Exception) {
            response.sendError(401)
        }

        if (Objects.isNull(username)) throw BadCredentialsException()

        val userDetails: UserDetails = authService.loadUserByUsername(username)

        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}

@Component
class JwtProvider {

    @Value("\${jwt.secretKey}")
     val secretKey: String? = null
    @Value("\${jwt.expireDate}")
    val expireDate: Int? = null


    fun generateToken(email: String?): String {

        val expireDate = Date(System.currentTimeMillis() + expireDate!! * 24 * 60 * 60 * 1000)

        return Jwts.builder()
            .subject(email)
            .issuedAt(Date())
            .expiration(expireDate)
            .signWith(key)
            .compact()
    }

    fun getSubject(token: String?): String {
        val payload: Claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parse(token)
            .getPayload() as Claims
        return payload.getSubject()
    }

    private val key: SecretKey
        get() {
            val bytes = Base64.getDecoder().decode(secretKey)
            return Keys.hmacShaKeyFor(bytes)
        }
}


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    @Lazy private val authService: AuthService,
    @Lazy private val jwtFilter: JwtFilter
) {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {

            httpSecurity.authorizeHttpRequests(
                Customizer { auth ->
                    auth
                        .anyRequest()
                        .authenticated()
                }
            )

            httpSecurity.csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            httpSecurity.sessionManagement { conf: SessionManagementConfigurer<HttpSecurity?> ->
                conf.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            httpSecurity.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            return httpSecurity.build()

    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }


    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setPasswordEncoder(passwordEncoder())
        provider.setUserDetailsService(authService)
        return provider
    }
}