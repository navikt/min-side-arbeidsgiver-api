package no.nav.arbeidsgiver.min_side.services.kontostatus

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.AzureAdTokenProvider
import no.nav.arbeidsgiver.min_side.infrastruktur.Miljø
import no.nav.arbeidsgiver.min_side.infrastruktur.Nullable
import no.nav.arbeidsgiver.min_side.infrastruktur.getOrComputeNullable
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient.Companion.apiPath
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient.Companion.ingress
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient.Companion.targetScope
import java.util.concurrent.TimeUnit

interface KontoregisterClient {
    suspend fun hentKontonummer(virksomhetsnummer: String): Kontooppslag?

    companion object {
        val ingress = Miljø.Sokos.kontoregisterBaseUrl
        val apiPath = "/kontoregister/api/v1/hent-kontonummer-for-organisasjon"
        val targetScope = Miljø.resolve(
            prod = { "api://prod-fss.okonomi.sokos-kontoregister/.default" },
            dev = { "api://dev-fss.okonomi.sokos-kontoregister-q2/.default" },
            other = { "" }
        )
    }
}

class KontoregisterClientImpl(
    defaultHttpClient: HttpClient,
    private val tokenProvider: AzureAdTokenProvider,
) : KontoregisterClient {

    private val httpClient = defaultHttpClient.config {
        expectSuccess = true
    }

    private val cache = Caffeine.newBuilder()
        .maximumSize(600000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .recordStats()
        .build<String, Nullable<Kontooppslag>>()

    override suspend fun hentKontonummer(virksomhetsnummer: String): Kontooppslag? {
        return cache.getOrComputeNullable("$KONTOREGISTER_CACHE_NAME-$virksomhetsnummer") {
            try {
                httpClient.get {
                    url {
                        takeFrom(ingress)
                        path(apiPath, virksomhetsnummer)
                    }
                    bearerAuth(
                        tokenProvider.token(targetScope).fold(
                            onSuccess = { it.accessToken },
                            onError = { throw RuntimeException("Failed to fetch token: ${it.status} ${it.error}") }
                        )
                    )
                }.body<Kontooppslag>()
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
@Serializable
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