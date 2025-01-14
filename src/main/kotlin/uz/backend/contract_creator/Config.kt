package uz.backend.contract_creator

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.AuditorAware
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.*


@Configuration
class WebMvcConfig : WebMvcConfigurer {

    @Bean
    fun userIdAuditorAware() =
        AuditorAware { Optional.ofNullable(SecurityContextHolder.getContext().getUserId()) }

    @Bean
    fun localeResolver() = SessionLocaleResolver().apply { setDefaultLocale(Locale("uz")) }

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasename("error")
    }


//    @Bean
//    fun messageSource() = ResourceBundleMessageSource().apply {
//        setDefaultEncoding(Charsets.UTF_8.name())
//        setBasename("message")
//    }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                //                        .allowedHeaders("*");
            }
        }
    }
}
@Configuration
@EnableAsync
class AsyncConfig {
}
