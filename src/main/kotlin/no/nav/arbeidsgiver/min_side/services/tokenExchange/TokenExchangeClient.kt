package no.nav.arbeidsgiver.min_side.services.tokenExchange

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
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
        val response = client.post(properties.tokenEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", TokenXProperties.GRANT_TYPE)
                        append("client_assertion_type", TokenXProperties.CLIENT_ASSERTION_TYPE)
                        append("subject_token_type", TokenXProperties.SUBJECT_TOKEN_TYPE)
                        append("subject_token", subjectToken)
                        append("client_assertion", clientAssertionTokenFactory.clientAssertion)
                        append("audience", audience)
                        append("client_id", properties.clientId)
                    }
                )
            )
        }
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Feil ved token exchange mot TokenX. Status=${response.status}")
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