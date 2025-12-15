package no.nav.arbeidsgiver.min_side.services.altinn

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.Miljø.Companion.clusterName
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.infrastruktur.getOrCompute
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService.Companion.audience
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService.Companion.ingress
import java.util.concurrent.TimeUnit

interface AltinnTilgangerService {
    companion object {
        val audience = "$clusterName:fager:arbeidsgiver-altinn-tilganger"
        val ingress = "http://arbeidsgiver-altinn-tilganger.fager"
    }

    suspend fun hentAltinnTilganger(token: String): AltinnTilganger
    suspend fun harTilgang(orgnr: String, tjeneste: String, token: String): Boolean
    suspend fun harOrganisasjon(orgnr: String, token: String): Boolean
}

class AltinnTilgangerServiceImpl(
    defaultHttpClient : HttpClient,
    private val tokenExchanger: TokenXTokenExchanger,
) : AltinnTilgangerService {

    private val httpClient = defaultHttpClient.config {
        install(ContentNegotiation) {
            json(defaultJson)
        }
    }

    /**
     * TODO:
     * denne cachen burde vært distribuert via valkeys eller lignende for å unngå cache miss
     * ved skalering og last balansering. I tillegg kunne vi fernet avhengighet til caffeine
     * men da bør vi ikke bruke token som cache key. se på altinn-tilganger cache mekanisme og låne den kanskje
     */
    private val cache: Cache<String, AltinnTilganger> =
        Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build()

    override suspend fun hentAltinnTilganger(token: String) =
        cache.getOrCompute(token) {
            hentAltinnTilgangerFraProxy(token)
        }

    override suspend fun harTilgang(orgnr: String, tjeneste: String, token: String) =
        hentAltinnTilganger(token).harTilgang(orgnr, tjeneste)

    override suspend fun harOrganisasjon(orgnr: String, token: String) = hentAltinnTilganger(token).harOrganisasjon(orgnr)

    private suspend fun hentAltinnTilgangerFraProxy(token: String): AltinnTilganger {
        val token = tokenExchanger.exchange(
            target = audience,
            userToken = token,
        ).fold(
            onSuccess = { it.accessToken },
            onError = { throw Exception("Failed to exchange token: ${it.error}") }
        )

        return httpClient.post {
            url {
                takeFrom(ingress)
                path("/altinn-tilganger")
            }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.body()
    }
}


@Serializable
data class AltinnTilganger(
    val isError: Boolean,
    val hierarki: List<AltinnTilgang>,
    val orgNrTilTilganger: Map<String, Set<String>>,
    val tilgangTilOrgNr: Map<String, Set<String>>,
) {
    @Serializable
    data class AltinnTilgang(
        val orgnr: String,
        val navn: String,
        val organisasjonsform: String,
        val altinn3Tilganger: Set<String>,
        val altinn2Tilganger: Set<String>,
        val underenheter: List<AltinnTilgang>,
    )

    val orgnrFlattened = hierarki.flatMap { flatten(it) { e -> e.orgnr } }

    fun harOrganisasjon(orgnr: String) = orgnrFlattened.any { it == orgnr }

    fun harTilgang(orgnr: String, tjeneste: String) = orgNrTilTilganger[orgnr]?.contains(tjeneste) ?: false
}

fun <T> flatten(
    e: AltinnTilgang,
    mapFn: (AltinnTilgang) -> T?
): List<T> = listOfNotNull(
    mapFn(e)
) + e.underenheter.flatMap { flatten(it, mapFn) }
