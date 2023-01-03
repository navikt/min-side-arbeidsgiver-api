package no.nav.arbeidsgiver.min_side.services.altinn

import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class AltinnService(
    private val tokenExchangeClient: TokenExchangeClient,
    private val altinnConfig: AltinnConfig,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
) {
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
    fun hentOrganisasjoner(fnr: String) =
        klient.hentOrganisasjoner(
            token,
            Subject(fnr),
            true
        ).toOrganisasjoner()

    @Cacheable(AltinnCacheConfig.ALTINN_TJENESTE_CACHE)
    fun hentOrganisasjonerBasertPaRettigheter(
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