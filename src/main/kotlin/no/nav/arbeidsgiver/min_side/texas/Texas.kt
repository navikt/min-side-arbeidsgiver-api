package no.nav.arbeidsgiver.notifikasjon.infrastruktur.texas

import com.fasterxml.jackson.annotation.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import no.nav.arbeidsgiver.min_side.defaultHttpClient

/**
 * Lånt med modifikasjoner fra https://github.com/nais/wonderwalled
 */

enum class IdentityProvider(val alias: String) {
    MASKINPORTEN("maskinporten"),
    AZURE_AD("azuread"),
    IDPORTEN("idporten"),
    TOKEN_X("tokenx"),
}

@JsonIgnoreProperties(ignoreUnknown = true)
sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Int,
    ) : TokenResponse() {
        override fun toString() = "TokenResponse.Success(accessToken: SECRET, expiresInSeconds: $expiresInSeconds)"
    }

    data class Error(
        val error: TokenErrorResponse,
        val status: HttpStatusCode,
    ) : TokenResponse()

    fun <R> fold(onSuccess: (Success) -> R, onError: (Error) -> R): R =
        when (this) {
            is Success -> onSuccess(this)
            is Error -> onError(this)
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenErrorResponse(
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenIntrospectionResponse(
    val active: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val error: String?,

    val acr: String? = null,
    val pid: String? = null,
    val azp: String? = null,
    /**
     * mapet "other" er alltid tomt. Dette er pga @JsonAnySetter er broken i jackson 2.17.0+
     * @JsonAnySetter er fikset i 2.18.1 https://github.com/FasterXML/jackson-databind/issues/4508
     */
    @JsonAnySetter @get:JsonAnyGetter
    val other: Map<String, Any?> = mutableMapOf(),
)

data class TexasAuthConfig(
    val tokenEndpoint: String,
    val tokenExchangeEndpoint: String,
    val tokenIntrospectionEndpoint: String,
) {
    companion object {
        fun nais() = TexasAuthConfig(
            tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT"),
            tokenExchangeEndpoint = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
            tokenIntrospectionEndpoint = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
        )
    }
}

interface AuthClient {
    suspend fun token(target: String, additionalParameters: Map<String, String> = mapOf()): TokenResponse

    suspend fun exchange(target: String, userToken: String): TokenResponse

    suspend fun introspect(accessToken: String): TokenIntrospectionResponse
}

class AuthClientImpl(
    private val config: TexasAuthConfig,
    private val provider: IdentityProvider,
    private val httpClient: HttpClient = defaultHttpClient {
        clientName = "TexasAuthClient"
    },
) : AuthClient {

    override suspend fun token(target: String, additionalParameters: Map<String, String>): TokenResponse = try {
        httpClient.submitForm(config.tokenEndpoint, parameters {
            set("target", target)
            set("identity_provider", provider.alias)
            additionalParameters.forEach { (key, value) -> set(key, value) }
        }).body<TokenResponse.Success>()
    } catch (e: ResponseException) {
        TokenResponse.Error(e.response.body<TokenErrorResponse>(), e.response.status)
    }

    override suspend fun exchange(target: String, userToken: String): TokenResponse = try {
        httpClient.submitForm(config.tokenExchangeEndpoint, parameters {
            set("target", target)
            set("user_token", userToken)
            set("identity_provider", provider.alias)
        }).body<TokenResponse.Success>()
    } catch (e: ResponseException) {
        TokenResponse.Error(e.response.body<TokenErrorResponse>(), e.response.status)
    }

    override suspend fun introspect(accessToken: String): TokenIntrospectionResponse =
        httpClient.submitForm(config.tokenIntrospectionEndpoint, parameters {
            set("token", accessToken)
            set("identity_provider", provider.alias)
        }).body()
}


fun ApplicationCall.bearerToken(): String? =
    request.authorization()
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.removePrefix("Bearer ")
        ?.removePrefix("bearer ")