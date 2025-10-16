package no.nav.arbeidsgiver.min_side.maskinporten

import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

interface MaskinportenTokenService {
    suspend fun currentAccessToken(): String
    suspend fun tokenRefreshingLoop()
}

class MaskinportenTokenServiceImpl(
    private val maskinportenClient: MaskinportenClient,
    private val meterRegistry: MeterRegistry,
) : MaskinportenTokenService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val tokenStore = AtomicReference<TokenResponseWrapper?>()

    override suspend fun currentAccessToken(): String {
        val storedToken = tokenStore.get()
        val token = if (storedToken != null && storedToken.percentageRemaining() > 20.0) {
            storedToken
        } else {
            logger.error("maskinporten access token almost expired. is refresh loop running? doing emergency fetch.")
            /* this shouldn't happen, as refresh loop above refreshes often */
            maskinportenClient.fetchNewAccessToken().also {
                tokenStore.set(it)
            }
        }

        return token.tokenResponse.accessToken
    }

    override suspend fun tokenRefreshingLoop() {
        meterRegistry.gauge(
            "maskinporten.token.expiry.seconds", tokenStore
        ) {
            it.get()?.expiresIn()?.seconds?.toDouble() ?: Double.NaN
        }

        while (true) {
            try {
                logger.info("sjekker om accesstoken er i ferd med å utløpe..")
                val token = tokenStore.get()
                if (token == null || token.percentageRemaining() < 50.0) {
                    val newToken = maskinportenClient.fetchNewAccessToken()
                    tokenStore.set(newToken)
                }
            } catch (e: Exception) {
                logger.error("refreshing maskinporten token failed with exception {}.", e.message, e)
            }
            delay(Duration.ofSeconds(30).toMillis())
        }
    }

    fun ready(): Boolean =
        when (val token = tokenStore.get()) {
            null -> false
            else -> token.expiresIn() > Duration.ZERO
        }

    fun alive(): Boolean =
        when (val token = tokenStore.get()) {
            null -> uptime() < Duration.ofMinutes(5)
            else -> token.percentageRemaining() > 10
        }

    private fun uptime(): Duration =
        Duration.ofMillis(
            ManagementFactory.getRuntimeMXBean().uptime
        )

}