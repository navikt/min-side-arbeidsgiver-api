package no.nav.arbeidsgiver.min_side.kontostatus

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.config.GittMiljø
import no.nav.arbeidsgiver.min_side.config.callIdIntercetor
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import java.net.SocketException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

@Component
class KontoregisterClient(
    @Value("\${sokos-kontoregister.baseUrl}") sokosKontoregisterBaseUrl: String?,
    restTemplateBuilder: RestTemplateBuilder,
    azureService: AzureService,
    gittMiljø: GittMiljø,
) {
    internal val restTemplate = restTemplateBuilder
        .rootUri(sokosKontoregisterBaseUrl)
        .additionalInterceptors(
            callIdIntercetor("nav-call-id"),
            ClientHttpRequestInterceptor { request, body, execution ->
                request.headers.setBearerAuth(
                    azureService.getAccessToken(gittMiljø.resolve(
                        prod = { "api://prod-fss.okonomi.sokos-kontoregister/.default" },
                        dev = { "api://dev-fss.okonomi.sokos-kontoregister-q2/.default" },
                        other = { "" }
                    ))
                )
                execution.execute(request, body)
            },
            retryInterceptor(
                3,
                250L,
                SocketException::class.java,
                SSLHandshakeException::class.java,
            )
        )
        .build()

    @Cacheable(KONTOREGISTER_CACHE_NAME)
    fun hentKontonummer(virksomhetsnummer: String): Kontooppslag? {
        return try {
            restTemplate.getForEntity(
                "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/{virksomhetsnummer}",
                Kontooppslag::class.java,
                mapOf("virksomhetsnummer" to virksomhetsnummer)
            ).body
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
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