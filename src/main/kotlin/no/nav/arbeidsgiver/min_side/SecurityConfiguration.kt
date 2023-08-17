package no.nav.arbeidsgiver.min_side

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.jwt.JwtClaimNames.AUD
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.web.SecurityFilterChain


// https://doc.nais.io/security/auth/concepts/tokens/#token-validation
// https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    @Value("\${idporten.client.id}") private val idportenClientId: String,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // Kotlin DSL requires import org.springframework.security.config.annotation.web.invoke
        http {
            oauth2ResourceServer { jwt { } }
            authorizeRequests {
                authorize("/internal/actuator/**", permitAll)
                authorize("/api/feature", permitAll)
                authorize("/api/**", authenticated)
                authorize(anyRequest, denyAll)
            }
            cors { disable() }
            csrf { disable() }
        }
        return http.build()
    }

    @Bean
    fun audValidator(resourceServerProperties: OAuth2ResourceServerProperties): OAuth2TokenValidator<Jwt?> {
        val allowedAudiences = resourceServerProperties.jwt.audiences
        return JwtClaimValidator<List<String>>(AUD) { aud  ->
            aud.any { allowedAudiences.contains(it) }
        }
    }

    @Bean
    fun acrValidator(): OAuth2TokenValidator<Jwt?> =
        JwtClaimValidator<List<String>>("acr") { it.contains("Level4")  || it.contains("idporten-loa-high")}
}