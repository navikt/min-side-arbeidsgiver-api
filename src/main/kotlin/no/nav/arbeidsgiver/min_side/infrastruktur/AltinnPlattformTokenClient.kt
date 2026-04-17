package no.nav.arbeidsgiver.min_side.infrastruktur

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

interface AltinnPlattformTokenClient {
    suspend fun token(scope: String): String
}

class AltinnPlattformTokenClientImpl(
    private val maskinporten: MaskinportenTokenProvider,
    defaultHttpClient: HttpClient,
    private val altinnPlatformBaseUrl: String = Miljø.Altinn.platformBaseUrl,
) : AltinnPlattformTokenClient {

    private val httpClient = defaultHttpClient.config {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    private data class CachedToken(val token: String, val expiresAt: Instant) {
        fun isValid(now: Instant = Instant.now()): Boolean =
            now.isBefore(expiresAt.minus(EXPIRY_BUFFER))
    }

    private val cache = ConcurrentHashMap<String, CachedToken>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    override suspend fun token(scope: String): String {
        cache[scope]?.takeIf { it.isValid() }?.let { return it.token }

        val mutex = locks.computeIfAbsent(scope) { Mutex() }
        return mutex.withLock {
            cache[scope]?.takeIf { it.isValid() }?.let { return it.token }
            val fresh = fetchPlatformToken(scope)
            cache[scope] = fresh
            fresh.token
        }
    }

    private suspend fun fetchPlatformToken(scope: String): CachedToken {
        val maskinportenToken = maskinporten.token(scope).fold(
            onSuccess = { it.accessToken },
            onError = {
                throw RuntimeException(
                    "Failed to fetch maskinporten token for scope '$scope': ${it.status} ${it.error}"
                )
            }
        )

        val rawToken = httpClient.get {
            url {
                takeFrom(altinnPlatformBaseUrl)
                path("/authentication/api/v1/exchange/maskinporten")
            }
            bearerAuth(maskinportenToken)
        }.body<String>().trim().trim('"')

        return CachedToken(token = rawToken, expiresAt = jwtExp(rawToken))
    }

    companion object {
        private val EXPIRY_BUFFER: Duration = Duration.ofSeconds(60)

        internal fun jwtExp(jwt: String): Instant {
            val parts = jwt.split('.')
            require(parts.size >= 2) { "Malformed JWT" }
            val payload = Base64.getUrlDecoder().decode(parts[1]).toString(Charsets.UTF_8)
            val exp = Json.parseToJsonElement(payload).jsonObject["exp"]
                ?: error("JWT missing 'exp' claim")
            return Instant.ofEpochSecond(exp.jsonPrimitive.long)
        }
    }
}
