package no.nav.arbeidsgiver.min_side

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(value = ["tokensupport.enabled"], havingValue = "true", matchIfMissing = true)
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework"])
@Configuration
class TokenSupportJwtConfig 