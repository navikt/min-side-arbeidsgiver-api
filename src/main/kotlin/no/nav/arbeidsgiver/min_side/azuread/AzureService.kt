package no.nav.arbeidsgiver.min_side.azuread

import com.github.benmanes.caffeine.cache.Expiry
import no.nav.arbeidsgiver.min_side.config.Cache
import java.util.concurrent.TimeUnit


class AzureService(
    private val azureClient: AzureClient,
) {
    private val cacheExpiryMarginSeconds = 60

    private val cache = Cache(maximumSize = 1000) {
        expireAfter(object : Expiry<String, AccessTokenEntry> {
            override fun expireAfterCreate(key: String, response: AccessTokenEntry, currentTime: Long): Long {
                return TimeUnit.SECONDS.toNanos(response.expiresInSeconds - cacheExpiryMarginSeconds)
            }

            override fun expireAfterUpdate(
                key: String,
                value: AccessTokenEntry,
                currentTime: Long,
                currentDuration: Long
            ): Long = currentDuration

            override fun expireAfterRead(
                key: String,
                value: AccessTokenEntry,
                currentTime: Long,
                currentDuration: Long
            ): Long = currentDuration
        })
    }

    suspend fun getAccessToken(scope: String): String {
        return cache.getOrCompute(scope) {
            azureClient.fetchToken(scope).let {
                AccessTokenEntry(
                    accessToken = it.accessToken,
                    expiresInSeconds = it.expiresIn.toLong()
                )
            }
        }.accessToken
    }
}

internal data class AccessTokenEntry(
    val accessToken: String,
    val expiresInSeconds: Long
)

data class AzureAdConfig(
    val openidTokenEndpoint: String,
    val clientId: String,
    val clientSecret: String,
)