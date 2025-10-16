package no.nav.arbeidsgiver.min_side.services.altinn

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import java.net.SocketException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

@Component
class AltinnService(
    restTemplateBuilder: RestTemplateBuilder,
    private val tokenExchangeClient: TokenExchangeClient,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    @Value("\${nais.cluster.name}") private val naisCluster: String,
) {

    private val cache: Cache<String, AltinnTilganger> = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build()

    private val restTemplate = restTemplateBuilder
        .additionalInterceptors(
            retryInterceptor(
                maxAttempts = 5,
                backoffPeriod = 250L,
                SocketException::class.java,
                SSLHandshakeException::class.java,
                ResourceAccessException::class.java,
            )
        ).build()

    fun hentAltinnTilganger() =
        cache.getIfPresent(authenticatedUserHolder.token) ?: run {
            hentAltinnTilgangerFraProxy().also {
                cache.put(authenticatedUserHolder.token, it)
            }
        }

    fun harTilgang(orgnr: String, tjeneste: String) = hentAltinnTilganger().harTilgang(orgnr, tjeneste)

    fun harOrganisasjon(orgnr: String) = hentAltinnTilganger().harOrganisasjon(orgnr)

    private fun hentAltinnTilgangerFraProxy(): AltinnTilganger {
        val token = tokenExchangeClient.exchange(
            authenticatedUserHolder.token,
            "$naisCluster:fager:arbeidsgiver-altinn-tilganger"
        ).access_token!!

        val response = restTemplate.exchange(
            RequestEntity
                .method(HttpMethod.POST, "http://arbeidsgiver-altinn-tilganger/altinn-tilganger")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $token")
                .build(),
            AltinnTilganger::class.java
        )

        return response.body!! // response != 200 => throws
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
