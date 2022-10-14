package no.nav.arbeidsgiver.min_side.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*;
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.arbeidsgiver.min_side.services.altinn.AltinnCacheConfig.ALTINN_CACHE;
import static no.nav.arbeidsgiver.min_side.services.altinn.AltinnCacheConfig.ALTINN_TJENESTE_CACHE;

@Slf4j
@Component
public class AltinnService {

    private final AltinnrettigheterProxyKlient klient;
    private final AuthenticatedUserHolder authenticatedUserHolder;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, AuthenticatedUserHolder authenticatedUserHolder) {
        this.authenticatedUserHolder = authenticatedUserHolder;
        this.klient = new AltinnrettigheterProxyKlient(
                new AltinnrettigheterProxyKlientConfig(
                        new ProxyConfig("min-side-arbeidsgiver-api", altinnConfig.getProxyUrl()),
                        new no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnConfig(
                                altinnConfig.getProxyFallbackUrl(),
                                altinnConfig.getAltinnHeader(),
                                altinnConfig.getAPIGwHeader()
                        )
                )
        );
    }

    @Cacheable(ALTINN_CACHE)
    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        return mapTo(
                klient.hentOrganisasjoner(
                        new SelvbetjeningToken(authenticatedUserHolder.getToken()),
                        new Subject(fnr),
                        true
                )
        );
    }

    @Cacheable(ALTINN_TJENESTE_CACHE)
    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        return mapTo(
                klient.hentOrganisasjoner(
                        new SelvbetjeningToken(authenticatedUserHolder.getToken()),
                        new Subject(fnr),
                        new ServiceCode(serviceKode),
                        new ServiceEdition(serviceEdition),
                        true
                )
        );
    }

    private List<Organisasjon> mapTo(List<AltinnReportee> altinnReportees) {
        return altinnReportees.stream().map(org ->
                Organisasjon.builder()
                        .name(org.getName())
                        .type(org.getType())
                        .parentOrganizationNumber(org.getParentOrganizationNumber())
                        .organizationNumber(org.getOrganizationNumber())
                        .organizationForm(org.getOrganizationForm())
                        .status(org.getStatus())
                        .build()
        ).collect(Collectors.toList());
    }
}
