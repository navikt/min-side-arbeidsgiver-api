package no.nav.arbeidsgiver.min_side.services.tokenExchange

import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpServerErrorException
import java.io.IOException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

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
            IOException::class.java, // lately been getting "GOAWAY received" as IOException, retry this as well
            SocketException::class.java,
            SSLHandshakeException::class.java,
            HttpServerErrorException.GatewayTimeout::class.java,
            HttpServerErrorException.ServiceUnavailable::class.java,
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