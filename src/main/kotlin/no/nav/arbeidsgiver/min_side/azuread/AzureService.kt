package no.nav.arbeidsgiver.min_side.azuread

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


//@Service
class AzureService(
    private val azureClient: AzureClient,
) {
    private val maxCachedEntries = 1000L
    private val cacheExpiryMarginSeconds = 60

    private val cache: Cache<String, AccessTokenEntry> = Caffeine.newBuilder()
        .expireAfter(object : Expiry<String, AccessTokenEntry> {
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
        .maximumSize(maxCachedEntries)
        .build()


    fun getAccessToken(scope: String): String {
        return cache.get(scope) {
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

@Configuration
@ConfigurationProperties("azuread")
class AzureADProperties(
    var openidTokenEndpoint: String = "",
    var clientId: String = "",
    var clientSecret: String = "",
)

data class AzureAdConfig(
    val openidTokenEndpoint: String,
    val clientId: String,
    val clientSecret: String,
)