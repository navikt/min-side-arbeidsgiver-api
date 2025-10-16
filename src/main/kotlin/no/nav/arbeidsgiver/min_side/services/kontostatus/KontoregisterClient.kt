package no.nav.arbeidsgiver.min_side.services.kontostatus

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.Nullable
import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.config.Miljø
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import no.nav.arbeidsgiver.min_side.getOrComputeNullable
import no.nav.arbeidsgiver.min_side.logger
import java.util.concurrent.TimeUnit

class KontoregisterClient(
    private val azureService: AzureService,
) {
    private val client = defaultHttpClient(configure = {
        expectSuccess = true
    })
    private val tokenScope = Miljø.resolve(
        prod = { "api://prod-fss.okonomi.sokos-kontoregister/.default" },
        dev = { "api://dev-fss.okonomi.sokos-kontoregister-q2/.default" },
        other = { "" }
    )
    private val logger = logger()

    private val cache = Caffeine.newBuilder()
        .maximumSize(600000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .recordStats()
        .build<String, Nullable<Kontooppslag>>()

    suspend fun hentKontonummer(virksomhetsnummer: String): Kontooppslag? {
        return cache.getOrComputeNullable("$KONTOREGISTER_CACHE_NAME-$virksomhetsnummer") {
            try {
                client.request("${Miljø.Sokos.kontoregisterBaseUrl}/kontoregister/api/v1/hent-kontonummer-for-organisasjon/${virksomhetsnummer}") {
                    method = HttpMethod.Get
                    bearerAuth(azureService.getAccessToken(tokenScope))
                }.let { response ->
                    if (response.status == HttpStatusCode.NotFound) {
                        null
                    }
                    response.body<Kontooppslag>()
                }
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.NotFound) {
                    return@getOrComputeNullable null
                }
                throw e
            }
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