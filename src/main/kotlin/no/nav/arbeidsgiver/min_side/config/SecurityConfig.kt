package no.nav.arbeidsgiver.min_side.config

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.defaultHttpClient

// https://doc.nais.io/security/auth/concepts/tokens/#token-validation
// https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html


class MsaJwtVerifier : JWTVerifier {
    private val client = defaultHttpClient()

    override fun verify(token: String?): DecodedJWT {
        if (token == null) throw JWTVerificationException("Token is null")
        return runBlocking {
            client.get(Environment.TokenX.tokenIntrospecionEndpint) {
                contentType(ContentType.Application.Json)
                setBody(TokenIntrospectionRequest(token))
            }.run {
                if (status != HttpStatusCode.OK) {
                    throw JWTVerificationException("Token introspection failed with status ${status}")
                }
                body<TokenIntrospectionResponse>()
            }.let {
                if (!it.active) {
                    throw JWTVerificationException("Token is not active: ${it.error}")
                }
                JWT.decode(token)
            }
        }
    }

    override fun verify(jwt: DecodedJWT?): DecodedJWT {
        return verify(jwt?.token)
    }


    data class TokenIntrospectionRequest(
        @JsonProperty
        val token: String,
        @JsonProperty
        val identity_provider: String = "tokenx"
    )


    data class TokenIntrospectionResponse(
        @JsonProperty
        val active: Boolean,
        @JsonProperty
        val error: String?,
    )
}