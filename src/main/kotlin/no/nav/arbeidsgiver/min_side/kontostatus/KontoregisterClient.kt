package no.nav.arbeidsgiver.min_side.kontostatus

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.config.Environment.Companion.sokosKontoregisterBaseUrl
import no.nav.arbeidsgiver.min_side.config.GittMiljø2
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

//@Component
class KontoregisterClient(
    private val azureService: AzureService,
) {
    private val client = defaultHttpClient()
    private val tokenScope = GittMiljø2.resolve(
        prod = { "api://prod-fss.okonomi.sokos-kontoregister/.default" },
        dev = { "api://dev-fss.okonomi.sokos-kontoregister-q2/.default" },
        other = { "" }
    )
    private val logger = logger()

    //TODO: cache this
    suspend fun hentKontonummer(virksomhetsnummer: String): Kontooppslag? {
        return client.request("${sokosKontoregisterBaseUrl}/kontoregister/api/v1/hent-kontonummer-for-organisasjon/${virksomhetsnummer}") {
            method = HttpMethod.Get
            bearerAuth(azureService.getAccessToken(tokenScope))
        }.let { response ->
            if (response.status == HttpStatusCode.NotFound) {
                logger.info("Fant ikke kontonummer for organisasjonsnummer $virksomhetsnummer")
                return null
            }
            response.body<Kontooppslag>()
        }
    }
}

/**
 * ref: https://github.com/navikt/sokos-kontoregister/blob/1ccc9c205a9b4e8f66dbe9c865383b256bcac26b/spec/kontoregister-v1-swagger2.json
 */
data class Kontooppslag(
    /**
     * Organisasjonsnummer
     */
    val mottaker: String,
    /**
     * Kontonummer
     */
    val kontonr: String,
)

const val KONTOREGISTER_CACHE_NAME = "kontoregister_cache"

@Configuration
class KontoregisterCacheConfig {

    @Bean
    fun kontoregisterCache() = CaffeineCache(
        KONTOREGISTER_CACHE_NAME,
        Caffeine.newBuilder()
            .maximumSize(600000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
            .build()
    )
}