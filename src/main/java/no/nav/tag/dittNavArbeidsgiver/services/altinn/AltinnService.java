package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*;
import no.nav.tag.dittNavArbeidsgiver.exceptions.TilgangskontrollException;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnCacheConfig.ALTINN_CACHE;
import static no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnCacheConfig.ALTINN_TJENESTE_CACHE;

@Slf4j
@Component
public class AltinnService {

    private final AltinnrettigheterProxyKlient klient;
    private final TokenUtils tokenUtils;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, TokenUtils tokenUtils ) {
        String altinnProxyUrl = altinnConfig.getProxyUrl();
        String altinnProxyFallbackUrl = altinnConfig.getProxyFallbackUrl();
        this.tokenUtils = tokenUtils;

        AltinnrettigheterProxyKlientConfig proxyKlientConfig = new AltinnrettigheterProxyKlientConfig(
                new ProxyConfig("ditt-nav-arbeidsgiver-api", altinnProxyUrl),
                new no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnConfig(
                        altinnProxyFallbackUrl,
                        altinnConfig.getAltinnHeader(),
                        altinnConfig.getAPIGwHeader()
                )
        );
        this.klient = new AltinnrettigheterProxyKlient(proxyKlientConfig);
    }

    @Cacheable(ALTINN_CACHE)
    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        try {
            return mapTo(klient.hentOrganisasjoner(
                   new SelvbetjeningToken(tokenUtils.getTokenForInnloggetBruker()),
                new Subject(fnr),
               true
                ));
        }  catch (Exception exception) {
            log.error("Feil fra Altinn: Exception: " + exception.getMessage());
            if(exception.getMessage().contains("403")){
                throw new TilgangskontrollException("bruker har ikke en aktiv altinn profil");
            }
            else{
                throw new AltinnException("Feil fra Altinn", exception);
            }
        }};


    @Cacheable(ALTINN_TJENESTE_CACHE)
    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        try {
            return mapTo(klient.hentOrganisasjoner(
                    new SelvbetjeningToken(tokenUtils.getTokenForInnloggetBruker()),
                    new Subject(fnr), new ServiceCode(serviceKode), new ServiceEdition(serviceEdition),
                    true
            ));
        }  catch (Exception exception) {
            log.error("Feil fra Altinn: Exception: " + exception.getMessage());
            if(exception.getMessage().contains("403")){
                throw new TilgangskontrollException("bruker har ikke en aktiv altinn profil");
            }
            else{
                throw new AltinnException("Feil fra Altinn", exception);
            }
        }};


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
