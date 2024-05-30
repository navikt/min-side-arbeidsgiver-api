package no.nav.arbeidsgiver.min_side.clients.azuread

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit



@Service
class AzureService(
    private val azureClient: AzureClient,
    private val azureADProperties: AzureADProperties,
) {
    private val maxCachedEntries = 1000L
    private val cacheExpiryMarginSeconds = 5

    private val cache: Cache<String, AccessTokenEntry> = Caffeine.newBuilder()
        .expireAfter(object : Expiry<String, AccessTokenEntry> {
            override fun expireAfterCreate(key: String, response: AccessTokenEntry, currentTime: Long): Long {
                return TimeUnit.SECONDS.toNanos(response.expiresInSeconds - cacheExpiryMarginSeconds)
            }
            override fun expireAfterUpdate(key: String, value: AccessTokenEntry, currentTime: Long, currentDuration: Long): Long = currentDuration
            override fun expireAfterRead(key: String, value: AccessTokenEntry, currentTime: Long, currentDuration: Long): Long = currentDuration
        })
        .maximumSize(maxCachedEntries)
        .build()


    fun getAccessToken(targetApp: String): String {
        return cache.get(targetApp) {
            azureClient.fetchToken(
                targetApp,
                JWTClaimsSet.Builder()
                    .issuer(azureADProperties.clientId)
                    .subject(azureADProperties.clientId)
                    .audience(azureADProperties.openidIssuer)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                    .jwtID(UUID.randomUUID().toString())
                    .build()
                    .sign(RSAKey.parse(azureADProperties.jwk))
                    .serialize(),
            ).let {
                AccessTokenEntry(
                    accessToken = it.accessToken,
                    expiresInSeconds = it.expiresIn.toLong()
                )
            }
        }.accessToken
    }

    private fun JWTClaimsSet.sign(rsaKey: RSAKey): SignedJWT =
        SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.keyID)
                .type(JOSEObjectType.JWT).build(),
            this
        ).apply {
            sign(RSASSASigner(rsaKey.toPrivateKey()))
        }

}

internal data class AccessTokenEntry(
    val accessToken: String,
    val expiresInSeconds: Long
)

@Configuration
@ConfigurationProperties("azuread")
class AzureADProperties(
    var tenantId: String = "",
    var openidTokenEndpoint: String = "",
    var clientId: String = "",
    var openidIssuer: String = "",
    var jwk: String = "",
)