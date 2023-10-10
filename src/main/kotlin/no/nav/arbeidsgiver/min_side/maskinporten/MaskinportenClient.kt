package no.nav.arbeidsgiver.min_side.maskinporten

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.arbeidsgiver.min_side.config.GittMiljø
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import java.time.Duration
import java.time.Instant
import java.util.*

interface MaskinportenClient {
    fun fetchNewAccessToken(): TokenResponseWrapper
}

@Component
@Profile("dev-gcp", "prod-gcp")
class MaskinportenClientImpl(
    val config: MaskinportenConfig,
    val gittMiljø: GittMiljø,
    restTemplateBuilder: RestTemplateBuilder,
): MaskinportenClient, InitializingBean {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = restTemplateBuilder.build()
    private lateinit var wellKnownResponse: WellKnownResponse

    override fun afterPropertiesSet() {
        wellKnownResponse = restTemplate.getForObject(config.wellKnownUrl, WellKnownResponse::class.java)!!
    }

    private fun createClientAssertion(): String {
        val now = Instant.now()
        val expire = now + Duration.ofSeconds(120)

        val claimsSet: JWTClaimsSet = JWTClaimsSet.Builder()
            .audience(wellKnownResponse.issuer)
            .issuer(config.clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expire))
            .notBeforeTime(Date.from(now))
            .claim("scope", config.scopes)
            .claim("resource", gittMiljø.resolve(prod = { "https://www.altinn.no/" }, other = { "https://tt02.altinn.no/" }))
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

    override fun fetchNewAccessToken(): TokenResponseWrapper {
        logger.info("henter ny accesstoken")
        val requestedAt = Instant.now()

        val tokenResponse = restTemplate.exchange(
            RequestEntity
                .method(HttpMethod.POST, wellKnownResponse.tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                    LinkedMultiValueMap(
                        mapOf(
                            "grant_type" to listOf("urn:ietf:params:oauth:grant-type:jwt-bearer"),
                            "assertion" to listOf(createClientAssertion())
                        )
                    )
                ),
            TokenResponse::class.java
        ).body!!

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

