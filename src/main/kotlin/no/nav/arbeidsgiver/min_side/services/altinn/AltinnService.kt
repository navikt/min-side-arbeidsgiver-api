package no.nav.arbeidsgiver.min_side.services.altinn

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*
import no.nav.arbeidsgiver.min_side.clients.retryInterceptor
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import org.apache.http.NoHttpResponseException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import java.lang.RuntimeException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

interface AltinnService {
    fun hentOrganisasjoner(fnr: String): List<Organisasjon>

    fun hentOrganisasjonerBasertPaRettigheter(
        fnr: String,
        serviceKode: String,
        serviceEdition: String
    ): List<Organisasjon>
}


@Component
@Profile("dev-gcp", "local")
class AltinnTilgangerService(
    restTemplateBuilder: RestTemplateBuilder,
    private val tokenExchangeClient: TokenExchangeClient,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    @Value("\${nais.cluster.name}") private val naisCluster: String,
) : AltinnService {

    private val restTemplate = restTemplateBuilder
        .additionalInterceptors(
            retryInterceptor(
                maxAttempts = 3,
                backoffPeriod = 250L,
                NoHttpResponseException::class.java,
                SocketException::class.java,
                SSLHandshakeException::class.java,
                ResourceAccessException::class.java,
            )
        )
        .build()

    override fun hentOrganisasjoner(fnr: String): List<Organisasjon> {
        val altinnTilganger = hentAltinnTilganger()
        val organisasjoner =  altinnTilganger.hierarki.flatMap { flattenUnderOrganisasjoner(it) }
        return organisasjoner
    }

    private fun hentAltinnTilganger(): AltinnTilgangerResponse {
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
            AltinnTilgangerResponse::class.java
        )

        return response.body!! // response != 200 => throws
    }

    private fun flattenUnderOrganisasjoner(
        altinnTilgang: AltinnTilgangerResponse.AltinnTilgang,
        parentOrgNr: String? = null
    ): List<Organisasjon> {
        val parent = Organisasjon(
            name = altinnTilgang.name,
            parentOrganizationNumber = parentOrgNr,
            organizationForm = altinnTilgang.organizationForm,
            organizationNumber = altinnTilgang.orgNr
        )
        val children = altinnTilgang.underenheter.flatMap { flattenUnderOrganisasjoner(it, parent.organizationNumber) }
        return listOf(parent) + children
    }

    override fun hentOrganisasjonerBasertPaRettigheter(
        fnr: String,
        serviceKode: String,
        serviceEdition: String
    ): List<Organisasjon> {

        val altinnTilganger = hentAltinnTilganger()
        val orgnrTilOrg = altinnTilganger.hierarki.flatMap{ flattenUnderOrganisasjoner(it) }.associateBy { it.organizationNumber }

        return altinnTilganger.tilgangTilOrgNr["${serviceKode}:${serviceEdition}"]?.let { orgnumre ->
            orgnumre.mapNotNull { orgNr -> orgnrTilOrg[orgNr] }
        } ?: emptyList()
    }
}

@Component
@Profile("prod-gcp")
class AltinnServiceImpl(
    restTemplateBuilder: RestTemplateBuilder,
    private val tokenExchangeClient: TokenExchangeClient,
    private val altinnConfig: AltinnConfig,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    @Value("\${nais.cluster.name}") private val naisCluster: String,
) : AltinnService {

    private val altinnTilgangerService = AltinnTilgangerService(
        restTemplateBuilder,
        tokenExchangeClient,
        authenticatedUserHolder,
        naisCluster
    )

    private val klient: AltinnrettigheterProxyKlient = AltinnrettigheterProxyKlient(
        AltinnrettigheterProxyKlientConfig(
            ProxyConfig("min-side-arbeidsgiver-api", altinnConfig.proxyUrl),
            no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnConfig(
                altinnConfig.proxyFallbackUrl,
                altinnConfig.altinnHeader,
                altinnConfig.APIGwHeader
            )
        )
    )

    @Cacheable(AltinnCacheConfig.ALTINN_CACHE)
    override fun hentOrganisasjoner(fnr: String) = try {
        altinnTilgangerService.hentOrganisasjoner(fnr)
    } catch (e: RuntimeException) {
        klient.hentOrganisasjoner(
            token,
            Subject(fnr),
            true
        ).filter {
            it.organizationForm == "BEDR"
                    || it.organizationForm == "AAFY"
                    || it.type == "Enterprise"
        }.toOrganisasjoner()
    }

    @Cacheable(AltinnCacheConfig.ALTINN_TJENESTE_CACHE)
    override fun hentOrganisasjonerBasertPaRettigheter(
        fnr: String,
        serviceKode: String,
        serviceEdition: String
    ) = try {
        altinnTilgangerService.hentOrganisasjonerBasertPaRettigheter(fnr, serviceKode, serviceEdition)
    } catch (e: RuntimeException) {
        klient.hentOrganisasjoner(
            token,
            Subject(fnr),
            ServiceCode(serviceKode),
            ServiceEdition(serviceEdition),
            true
        ).filter {
            it.organizationForm == "BEDR"
                    || it.organizationForm == "AAFY"
                    || it.type == "Enterprise"
        }.toOrganisasjoner()
    }

    private val token: Token
        get() = TokenXToken(
            tokenExchangeClient.exchange(
                authenticatedUserHolder.token,
                altinnConfig.proxyAudience
            ).access_token!!
        )

    private fun List<AltinnReportee>.toOrganisasjoner() =
        mapNotNull {
            val organizationNumber = it.organizationNumber
            val organizationForm = it.organizationForm
            if (organizationNumber == null || organizationForm == null)
                null
            else
                Organisasjon(
                    name = it.name,
                    parentOrganizationNumber = it.parentOrganizationNumber,
                    organizationNumber = organizationNumber,
                    organizationForm = organizationForm,
                )
        }
}


@JsonIgnoreProperties(ignoreUnknown = true)
private data class AltinnTilgangerResponse(
    val isError: Boolean,
    val hierarki: List<AltinnTilgang>,
    val orgNrTilTilganger: Map<String, Set<String>>,
    val tilgangTilOrgNr: Map<String, Set<String>>,
) {
    data class AltinnTilgang(
        val orgNr: String,
        val altinn3Tilganger: Set<String>,
        val altinn2Tilganger: Set<String>,
        val underenheter: List<AltinnTilgang>,
        val name: String,
        val organizationForm: String,
    )
}

