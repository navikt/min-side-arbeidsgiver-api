package no.nav.arbeidsgiver.min_side.services.altinn

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.config.GittMiljø2
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import java.util.concurrent.TimeUnit

class AltinnService(
    private val tokenExchangeClient: TokenExchangeClient,
) {

    private val client = defaultHttpClient()
    private val audience = "${
        GittMiljø2.resolve(
            prod = { System.getenv("NAIS_CLUSTER_NAME") },
            dev = { System.getenv("NAIS_CLUSTER_NAME") },
            other = { "local" }
        )
    }:fager:arbeidsgiver-altinn-tilganger"

    private val cache: Cache<String, AltinnTilganger> =
        Caffeine.newBuilder() //TODO: kopier denne rundt om kring der det trengs
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build()

    suspend fun hentAltinnTilganger(authenticatedUserHolder: AuthenticatedUserHolder) =
        cache.getIfPresent(authenticatedUserHolder.token) ?: run {
            hentAltinnTilgangerFraProxy(authenticatedUserHolder).also {
                cache.put(authenticatedUserHolder.token, it)
            }
        }

    suspend fun harTilgang(orgnr: String, tjeneste: String, authenticatedUserHolder: AuthenticatedUserHolder) = hentAltinnTilganger(authenticatedUserHolder).harTilgang(orgnr, tjeneste)

    suspend fun harOrganisasjon(orgnr: String, authenticatedUserHolder: AuthenticatedUserHolder) = hentAltinnTilganger(authenticatedUserHolder).harOrganisasjon(orgnr)

    private suspend fun hentAltinnTilgangerFraProxy(authenticatedUserHolder: AuthenticatedUserHolder): AltinnTilganger {
        val token = tokenExchangeClient.exchange(
            subjectToken = authenticatedUserHolder.token,
            audience = audience,
        ).access_token!!

        return client.post("http://arbeidsgiver-altinn-tilganger/altinn-tilganger") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.body()
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class AltinnTilganger(
    val isError: Boolean,
    val hierarki: List<AltinnTilgang>,
    val orgNrTilTilganger: Map<String, Set<String>>,
    val tilgangTilOrgNr: Map<String, Set<String>>,
) {
    data class AltinnTilgang(
        val orgnr: String,
        val navn: String,
        val organisasjonsform: String,
        val altinn3Tilganger: Set<String>,
        val altinn2Tilganger: Set<String>,
        val underenheter: List<AltinnTilgang>,
    )

    @get:JsonIgnore
    val orgnrFlattened = hierarki.flatMap { flatten(it) { e -> e.orgnr } }

    fun harOrganisasjon(orgnr: String) = orgnrFlattened.any { it == orgnr }

    fun harTilgang(orgnr: String, tjeneste: String) = orgNrTilTilganger[orgnr]?.contains(tjeneste) ?: false
}

private fun <T> flatten(
    e: AltinnTilgang,
    mapFn: (AltinnTilgang) -> T?
): List<T> = listOfNotNull(
    mapFn(e)
) + e.underenheter.flatMap { flatten(it, mapFn) }
