package no.nav.arbeidsgiver.min_side.azuread

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.ResourceAccessException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

@Component
class AzureClient(
    restTemplateBuilder: RestTemplateBuilder,
    private val azureADProperties: AzureADProperties,
) {
    private val restTemplate = restTemplateBuilder
        .additionalInterceptors(
            retryInterceptor(
                maxAttempts = 3,
                backoffPeriod = 250L,
                SocketException::class.java,
                SSLHandshakeException::class.java,
                ResourceAccessException::class.java,
            )
        )
        .build()

    fun fetchToken(targetApp: String, clientAssertion: String): TokenResponse {
        val request = HttpEntity<MultiValueMap<String, String>>(
            LinkedMultiValueMap(
                mapOf(
                    "tenant" to listOf(azureADProperties.tenantId),
                    "client_id" to listOf(azureADProperties.clientId),
                    "scope" to listOf("api://$targetApp/.default"),
                    "client_assertion_type" to listOf("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                    "client_assertion" to listOf(clientAssertion),
                    "grant_type" to listOf("client_credentials"),
                )
            ),
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            }
        )

        return restTemplate.postForEntity(
            azureADProperties.openidTokenEndpoint,
            request,
            TokenResponse::class.java
        ).body!!
    }
}

data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("ext_expires_in") val extExpiresIn: Int
)
