package no.nav.arbeidsgiver.min_side.controller

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

class AuthenticatedUserHolder(private val call: ApplicationCall) {
    val fnr: String
        get() = jwt.payload.getClaim("pid").asString()

    val token: String
        get() = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: throw RuntimeException("No Authorization header found")

    private val jwt: JWTCredential
        get() = call.principal<JWTCredential>()
            ?: throw RuntimeException("No valid JWT token found")
}