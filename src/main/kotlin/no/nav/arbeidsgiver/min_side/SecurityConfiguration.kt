package no.nav.arbeidsgiver.min_side

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


// https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

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
    fun jwtDecoder(resourceServerProperties: OAuth2ResourceServerProperties): JwtDecoder {
        val issuerUri = resourceServerProperties.jwt.issuerUri
        val allowedAudiences = resourceServerProperties.jwt.audiences

        return JwtDecoders.fromIssuerLocation<NimbusJwtDecoder>(issuerUri).apply {
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    //JwtTimestampValidator(),
                    JwtIssuerValidator(issuerUri),
                    JwtClaimValidator<String>("acr") { it == "Level4" || it == "idporten-loa-high" },
                    JwtClaimValidator(AUD) { aud: List<String>? ->
                        aud?.any { allowedAudiences.contains(it) } ?: false
                    },
                    //JwtClaimValidator<String>("azp") {  it?.isNotBlank() ?: false},
                    //JwtClaimValidator<String>("azp_name") { it?.isNotBlank() ?: false },
                )
            )
        }
    }
}