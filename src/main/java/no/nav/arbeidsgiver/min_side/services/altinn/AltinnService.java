package no.nav.arbeidsgiver.min_side.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.error.exceptions.AltinnException;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*;
import no.nav.arbeidsgiver.min_side.exceptions.TilgangskontrollException;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static no.nav.arbeidsgiver.min_side.services.altinn.AltinnCacheConfig.ALTINN_CACHE;
import static no.nav.arbeidsgiver.min_side.services.altinn.AltinnCacheConfig.ALTINN_TJENESTE_CACHE;

@Slf4j
@Component
public class AltinnService {

    private final AltinnrettigheterProxyKlient klient;
    private final AuthenticatedUserHolder tokenUtils;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, AuthenticatedUserHolder tokenUtils) {
        this.tokenUtils = tokenUtils;
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
        return medFeilFraAltinnHåndtert(() -> mapTo(
                klient.hentOrganisasjoner(
                        new SelvbetjeningToken(tokenUtils.getToken()),
                        new Subject(fnr),
                        true
                )
        ));
    }

    @Cacheable(ALTINN_TJENESTE_CACHE)
    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        return medFeilFraAltinnHåndtert(() -> mapTo(
                klient.hentOrganisasjoner(
                        new SelvbetjeningToken(tokenUtils.getToken()),
                        new Subject(fnr),
                        new ServiceCode(serviceKode),
                        new ServiceEdition(serviceEdition),
                        true
                )
        ));
    }

    private List<Organisasjon> medFeilFraAltinnHåndtert(Supplier<List<Organisasjon>> fn) {
        try {
            return fn.get();
        } catch (AltinnException exception) {
            if (exception.getProxyError().getHttpStatus() == 403) {
                throw new TilgangskontrollException("bruker har ikke en aktiv altinn profil", exception);
            } else {
                throw exception;
            }
        } catch (Exception exception) {
            if (exception.getMessage().contains("403")) {
                throw new TilgangskontrollException("bruker har ikke en aktiv altinn profil", exception);
            } else {
                throw exception;
            }
        }
    }

    private List<Organisasjon> mapTo(List<AltinnReportee> altinnReportees) {
        return altinnReportees.stream().map(org -> {
                    Organisasjon altinnOrganisasjon = new Organisasjon();
                    altinnOrganisasjon.setName(org.getName());
                    altinnOrganisasjon.setType(org.getType());
                    altinnOrganisasjon.setParentOrganizationNumber(org.getParentOrganizationNumber());
                    altinnOrganisasjon.setOrganizationNumber(org.getOrganizationNumber());
                    altinnOrganisasjon.setOrganizationForm(org.getOrganizationForm());
                    altinnOrganisasjon.setStatus(org.getStatus());

                    return altinnOrganisasjon;
                }
        ).collect(Collectors.toList());
    }
}
