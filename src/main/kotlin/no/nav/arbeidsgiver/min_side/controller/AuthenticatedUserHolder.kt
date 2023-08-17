package no.nav.arbeidsgiver.min_side.controller

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component


@Component
class AuthenticatedUserHolder {
    val fnr: String
        get() = if (authentication is JwtAuthenticationToken) {
            (authentication as JwtAuthenticationToken).token.getClaimAsString("pid")
        } else {
            throw RuntimeException("no valid token. auth: ${authentication.javaClass}")
        }

    val token: String
        get() = if (authentication is JwtAuthenticationToken) {
            (authentication as JwtAuthenticationToken).token.getClaimAsString("pid")
        } else {
            // used by feature toggle service
            "anonymous"
        }

    private val authentication: Authentication
        get() = SecurityContextHolder.getContext().authentication
}