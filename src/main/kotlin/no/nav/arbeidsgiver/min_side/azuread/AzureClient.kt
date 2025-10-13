package no.nav.arbeidsgiver.min_side.azuread

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.defaultHttpClient

class AzureClient(
    private val azureAdConfig: AzureAdConfig,
) {
    private val client = defaultHttpClient()

    suspend fun fetchToken(scope: String): TokenResponse {
        return client.request(azureAdConfig.openidTokenEndpoint) {
            method = HttpMethod.Post
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "client_credentials")
                        append("client_id", azureAdConfig.clientId)
                        append("client_secret", azureAdConfig.clientSecret)
                        append("scope", scope)
                    }
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