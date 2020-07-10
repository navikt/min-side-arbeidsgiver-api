package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.finn.unleash.Unleash;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.AltinnReportee;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.Subject;
import no.nav.security.oidc.context.TokenContext;
import no.nav.tag.dittNavArbeidsgiver.exceptions.TilgangskontrollException;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.Role;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnCacheConfig.ALTINN_CACHE;
import static no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnCacheConfig.ALTINN_TJENESTE_CACHE;

@Slf4j
@Component
public class AltinnService {

    private static final int ALTINN_ORG_PAGE_SIZE = 500;

    private final String altinnProxyUrl;
    private final String altinnProxyFallbackUrl;
    private final AltinnrettigheterProxyKlient klient;
    private final TokenUtils tokenUtils;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, TokenUtils tokenUtils) {
        this.altinnProxyUrl = altinnConfig.getProxyUrl();
        this.altinnProxyFallbackUrl = altinnConfig.getProxyFallbackUrl();
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
        String filterParamVerdi = "Type+ne+'Person'+and+Status+eq+'Active'";
        Map<String, String> parametre = new ConcurrentHashMap<>();
        parametre.put("$filter", filterParamVerdi);
        try {
            return getReporteesFromAltinnViaProxy(
                tokenUtils.getSelvbetjeningTokenContext(),
                new Subject(fnr),
                parametre,
                altinnProxyUrl,
                ALTINN_ORG_PAGE_SIZE
                );
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

            Map<String, String> parametre = new ConcurrentHashMap<>();
            parametre.put("serviceCode", serviceKode);
            parametre.put("serviceEdition", serviceEdition);
            return getReporteesFromAltinnViaProxy(
                    tokenUtils.getSelvbetjeningTokenContext(),
                    new Subject(fnr),
                    parametre,
                    altinnProxyUrl,
                    ALTINN_ORG_PAGE_SIZE
            );
    }


    List<Organisasjon> getReporteesFromAltinnViaProxy(
            TokenContext tokenContext,
            Subject subject,
            Map<String, String> parametre,
            String url,
            int pageSize
    ) {
        Set<Organisasjon> response = new HashSet<>();
        int pageNumber = 0;
        boolean hasMore = true;

        while (hasMore) {
            pageNumber++;
            try {
                parametre.put("$top", String.valueOf(pageSize));
                parametre.put("$skip", String.valueOf(((pageNumber - 1) * pageSize)));
                List<Organisasjon> collection = mapTo(klient.hentOrganisasjoner(tokenContext, subject, parametre));
                response.addAll(collection);
                hasMore = collection.size() >= pageSize;
            } catch (Exception exception) {
                log.error("Feil fra Altinn: Exception: " + exception.getMessage());
                if(exception.getMessage().contains("403")){
                    throw new TilgangskontrollException("bruker har ikke en aktiv altinn profil");
                }
                else{
                    throw new AltinnException("Feil fra Altinn", exception);
                }

            }
        }
        return new ArrayList<>(response);
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
