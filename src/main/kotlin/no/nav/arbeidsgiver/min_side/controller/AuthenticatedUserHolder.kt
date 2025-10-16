package no.nav.arbeidsgiver.min_side.controller

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component


@Component
class AuthenticatedUserHolder {
    val fnr: String
        get() = jwt.getClaimAsString("pid")

    val token: String
        get() = jwt.tokenValue

    private val jwt: Jwt
        get() = try {
            (SecurityContextHolder.getContext().authentication as JwtAuthenticationToken).token
        } catch (e: Exception) {
            throw RuntimeException("no valid token. auth: ${SecurityContextHolder.getContext().authentication.javaClass}")
        }
}