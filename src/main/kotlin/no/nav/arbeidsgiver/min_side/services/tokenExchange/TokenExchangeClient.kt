package no.nav.arbeidsgiver.min_side.services.tokenExchange

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.defaultHttpClient

interface TokenExchangeClient {
    suspend fun exchange(subjectToken: String, audience: String): TokenXToken
}

class TokenExchangeClientImpl(
    private val properties: TokenXProperties,
    private val clientAssertionTokenFactory: ClientAssertionTokenFactory,
) : TokenExchangeClient {

    private val client = defaultHttpClient()

    override suspend fun exchange(subjectToken: String, audience: String): TokenXToken {
        val response = client.post(properties.tokenEndpoint){
            setBody(mapOf(
                "grant_type" to listOf(TokenXProperties.GRANT_TYPE),
                "client_assertion_type" to listOf(TokenXProperties.CLIENT_ASSERTION_TYPE),
                "subject_token_type" to listOf(TokenXProperties.SUBJECT_TOKEN_TYPE),
                "subject_token" to listOf(subjectToken),
                "client_assertion" to listOf(clientAssertionTokenFactory.clientAssertion),
                "audience" to listOf(audience),
                "client_id" to listOf(properties.clientId)
            ))
            contentType(ContentType.Application.Json)
        }
        if (response.status != HttpStatusCode.OK){
            throw RuntimeException("Feil ved token exchange mot TokenX. Status=${response.status} message =${response.body<String>()}")
        }

        return response.body()
    }
}

data class TokenXToken(
    var access_token: String? = null,
    var issued_token_type: String? = null,
    var token_type: String? = null,
    var expires_in: Int = 0
)