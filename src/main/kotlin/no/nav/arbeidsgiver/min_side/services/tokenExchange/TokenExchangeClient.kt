package no.nav.arbeidsgiver.min_side.services.tokenExchange

import no.nav.arbeidsgiver.min_side.clients.retryInterceptor
import org.apache.http.NoHttpResponseException
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

interface TokenExchangeClient {
    fun exchange(subjectToken: String, audience: String): TokenXToken
}

@Profile("local", "dev-gcp", "prod-gcp")
@Component
class TokenExchangeClientImpl(
    private val properties: TokenXProperties,
    private val clientAssertionTokenFactory: ClientAssertionTokenFactory,
    restTemplateBuilder: RestTemplateBuilder
) : TokenExchangeClient {

    private val restTemplate = restTemplateBuilder.additionalInterceptors(
        retryInterceptor(
            3,
            250L,
            NoHttpResponseException::class.java
        )
    ).build()

    override fun exchange(subjectToken: String, audience: String): TokenXToken {
        val request = HttpEntity<MultiValueMap<String, String>>(
            LinkedMultiValueMap(
                mapOf(
                    "grant_type" to listOf(TokenXProperties.GRANT_TYPE),
                    "client_assertion_type" to listOf(TokenXProperties.CLIENT_ASSERTION_TYPE),
                    "subject_token_type" to listOf(TokenXProperties.SUBJECT_TOKEN_TYPE),
                    "subject_token" to listOf(subjectToken),
                    "client_assertion" to listOf(clientAssertionTokenFactory.clientAssertion),
                    "audience" to listOf(audience),
                    "client_id" to listOf(properties.clientId)
                )
            ),
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            }
        )
        return restTemplate.postForEntity(properties.tokenEndpoint, request, TokenXToken::class.java).body!!
    }
}

@Profile("labs")
@Component
class TokenExchangeClientStub : TokenExchangeClient {
    override fun exchange(subjectToken: String, audience: String): TokenXToken {
        return TokenXToken(
            access_token = "fake-access-token",
            token_type = "fake-token-type",
            issued_token_type = "fake-issued-token-type",
            expires_in = 3600,
        )
    }
}