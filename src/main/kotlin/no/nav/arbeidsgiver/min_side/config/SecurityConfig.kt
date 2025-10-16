package no.nav.arbeidsgiver.min_side.config

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.jwt.JwtClaimNames.AUD
import org.springframework.security.web.SecurityFilterChain

// https://doc.nais.io/security/auth/concepts/tokens/#token-validation
// https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // Kotlin DSL requires import org.springframework.security.config.annotation.web.invoke
        http {
            oauth2ResourceServer { jwt { } }
            authorizeHttpRequests {
                authorize("/internal/actuator/**", permitAll)
                authorize("/api/**", authenticated)
                authorize(anyRequest, denyAll)
            }
            cors { disable() }
            csrf { disable() }
        }
        return http.build()
    }

    @Bean
    fun jwtDecoder(resourceServerProperties: OAuth2ResourceServerProperties): JwtDecoder {
        val issuerUri = resourceServerProperties.jwt.issuerUri
        val allowedAudiences = resourceServerProperties.jwt.audiences
        val validAcrClaims = listOf("Level4", "idporten-loa-high")

        return JwtDecoders.fromIssuerLocation<NimbusJwtDecoder>(issuerUri).apply {
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(issuerUri),
                    JwtClaimValidator<String>("acr") { validAcrClaims.contains(it) },
                    JwtClaimValidator(AUD) { aud: List<String> -> aud.any { allowedAudiences.contains(it) }},
                )
            )
        }
    }
}