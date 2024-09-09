package no.nav.arbeidsgiver.min_side.services.altinn

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*
import no.nav.arbeidsgiver.min_side.clients.retryInterceptor
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import org.apache.http.NoHttpResponseException
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
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
        var response = restTemplate.postForEntity("/altinn-tilganger", "", AltinnTilgangerClientResponse::class.java)

        return listOf()
    }

    override fun hentOrganisasjonerBasertPaRettigheter(
        fnr: String,
        serviceKode: String,
        serviceEdition: String
    ): List<Organisasjon> {
        TODO("Not yet implemented")
    }
}

@Component
@Profile("prod-gcp")
class AltinnServiceImpl(
    private val tokenExchangeClient: TokenExchangeClient,
    private val altinnConfig: AltinnConfig,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
) : AltinnService {
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
    override fun hentOrganisasjoner(fnr: String) =
        klient.hentOrganisasjoner(
            token,
            Subject(fnr),
            true
        ).toOrganisasjoner()

    @Cacheable(AltinnCacheConfig.ALTINN_TJENESTE_CACHE)
    override fun hentOrganisasjonerBasertPaRettigheter(
        fnr: String,
        serviceKode: String,
        serviceEdition: String
    ) = klient.hentOrganisasjoner(
        token,
        Subject(fnr),
        ServiceCode(serviceKode),
        ServiceEdition(serviceEdition),
        true
    ).toOrganisasjoner()

    private val token: Token
        get() = TokenXToken(
            tokenExchangeClient.exchange(
                authenticatedUserHolder.token,
                altinnConfig.proxyAudience
            ).access_token!!
        )

    private fun List<AltinnReportee>.toOrganisasjoner() = map {
        Organisasjon(
            name = it.name,
            type = it.type,
            parentOrganizationNumber = it.parentOrganizationNumber,
            organizationNumber = it.organizationNumber,
            organizationForm = it.organizationForm,
            status = it.status,
        )
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
private data class AltinnTilgangerClientResponse(
    val isError: Boolean,
    val orgNrTilTilganger: Map<String, List<String>>,
)