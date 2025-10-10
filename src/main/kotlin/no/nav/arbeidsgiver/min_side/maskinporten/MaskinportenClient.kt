package no.nav.arbeidsgiver.min_side.maskinporten

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.config.Miljø
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

interface MaskinportenClient {
    suspend fun fetchNewAccessToken(): TokenResponseWrapper
}

class MaskinportenClientImpl(
    val config: MaskinportenConfig2,
) : MaskinportenClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = defaultHttpClient()
    private var wellKnownResponse: WellKnownResponse? = null

    private suspend fun createClientAssertion(): String {
        val wellKnownResponse = getWellKnownResponse()
        val now = Instant.now()
        val expire = now + Duration.ofSeconds(120)

        val claimsSet: JWTClaimsSet = JWTClaimsSet.Builder()
            .audience(wellKnownResponse.issuer)
            .issuer(config.clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expire))
            .notBeforeTime(Date.from(now))
            .claim("scope", config.scopes)
            .claim(
                "resource",
                Miljø.resolve(prod = { "https://www.altinn.no/" }, other = { "https://tt02.altinn.no/" })
            )
            .jwtID(UUID.randomUUID().toString())
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(config.privateJwkRsa.keyID)
                .build(),
            claimsSet
        )
        signedJWT.sign(config.jwsSigner)
        return signedJWT.serialize()
    }

    private suspend fun getWellKnownResponse(): WellKnownResponse {
        if (wellKnownResponse == null) {
            wellKnownResponse = client.get(config.wellKnownUrl).body()
        }
        return wellKnownResponse!!
    }

    override suspend fun fetchNewAccessToken(): TokenResponseWrapper {
        logger.info("henter ny accesstoken")
        val requestedAt = Instant.now()

        val tokenResponse = client.get(getWellKnownResponse().tokenEndpoint) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "grant_type" to listOf("urn:ietf:params:oauth:grant-type:jwt-bearer"),
                    "assertion" to listOf(createClientAssertion())
                )
            )
        }.body<TokenResponse>()

        logger.info("Fetched new access token. Expires in {} seconds.", tokenResponse.expiresInSeconds)

        return TokenResponseWrapper(
            requestedAt = requestedAt,
            tokenResponse = tokenResponse,
        )
    }
}

/**
 * e.g.
 * {
 *      "issuer": "https://ver2.maskinporten.no/",
 *      "token_endpoint": "https://ver2.maskinporten.no/token",
 *      "jwks_uri": "https://ver2.maskinporten.no/jwk",
 *      "token_endpoint_auth_methods_supported": [
 *          "private_key_jwt"
 *      ],
 *      "grant_types_supported": [
 *          "urn:ietf:params:oauth:grant-type:jwt-bearer"
 *      ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
private data class WellKnownResponse(
    @JsonProperty("issuer") val issuer: String,
    @JsonProperty("token_endpoint") val tokenEndpoint: String
)

/**
 * {
 *   "access_token" : "IxC0B76vlWl3fiQhAwZUmD0hr_PPwC9hSIXRdoUslPU=",
 *   "token_type" : "Bearer",
 *   "expires_in" : 599,
 *   "scope" : "difitest:test1"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresInSeconds: Long,
    @JsonProperty("scope") val scope: String,
) {
    val expiresIn: Duration = Duration.ofSeconds(expiresInSeconds)
}

data class TokenResponseWrapper(
    val requestedAt: Instant,
    val tokenResponse: TokenResponse,
) {
    private val validFor = tokenResponse.expiresIn
    private val expiresAt = requestedAt + validFor

    fun expiresIn(now: Instant = Instant.now()): Duration = Duration.between(now, expiresAt)

    fun percentageRemaining(now: Instant = Instant.now()): Double {
        val timeToExpire = expiresIn(now).seconds.toDouble()
        return if (timeToExpire <= 0)
            0.0
        else
            100.0 * (timeToExpire / validFor.seconds)
    }
}

