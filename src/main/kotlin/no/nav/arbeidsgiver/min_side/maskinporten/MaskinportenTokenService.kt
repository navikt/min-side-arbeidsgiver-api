package no.nav.arbeidsgiver.min_side.maskinporten

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

interface MaskinportenTokenService {
    fun currentAccessToken(): String
}

//@Component
@Profile("dev-gcp", "prod-gcp")
class MaskinportenTokenServiceImpl(
    private val maskinportenClient: MaskinportenClient,
    private val meterRegistry: MeterRegistry,
): MaskinportenTokenService, InitializingBean {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val tokenStore = AtomicReference<TokenResponseWrapper?>()

    override fun currentAccessToken(): String {
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

    override fun afterPropertiesSet() {
        meterRegistry.gauge(
            "maskinporten.token.expiry.seconds", tokenStore
        ) {
            it.get()?.expiresIn()?.seconds?.toDouble() ?: Double.NaN
        }

        Thread {
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
                Thread.sleep(Duration.ofSeconds(30).toMillis())
            }
        }.start()
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