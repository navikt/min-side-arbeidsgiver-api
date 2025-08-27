package no.nav.arbeidsgiver.min_side.azuread

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

//@Component
class AzureClient(
    private val azureAdConfig: AzureAdConfig,
) {
    private val client = HttpClient(CIO) {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    suspend fun fetchToken(scope: String): TokenResponse {
        return client.request(azureAdConfig.openidTokenEndpoint) {
            method = HttpMethod.Post
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                mapOf(
                    "grant_type" to "client_credentials",
                    "client_id" to azureAdConfig.clientId,
                    "client_secret" to azureAdConfig.clientSecret,
                    "scope" to scope,
                )
            )
        }.body<TokenResponse>()
    }
}

data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("ext_expires_in") val extExpiresIn: Int
)