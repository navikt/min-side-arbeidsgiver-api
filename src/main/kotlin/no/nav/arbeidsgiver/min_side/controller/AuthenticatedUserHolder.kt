package no.nav.arbeidsgiver.min_side.controller

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.stereotype.Component

@Component
class AuthenticatedUserHolder(private val requestContextHolder: TokenValidationContextHolder) {
    val fnr: String
        get() = jwtToken.jwtTokenClaims.getStringClaim("pid")
    val token: String
        get() = jwtToken.tokenAsString

    private val jwtToken: JwtToken
        get() = requestContextHolder.tokenValidationContext
            .firstValidToken
            .orElseThrow {
                NoSuchElementException(
                    "no valid token. how did you get so far without a valid token?"
                )
            }

    companion object {
        const val TOKENX = "tokenx"
        const val ACR_CLAIM_OLD = "acr=Level4"
        const val ACR_CLAIM_NEW = "acr=idporten-loa-high"
    }
}