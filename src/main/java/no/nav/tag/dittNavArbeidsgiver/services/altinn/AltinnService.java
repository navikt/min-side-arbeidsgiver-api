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
    private static final int ALTINN_ROLE_PAGE_SIZE = 50;

    private final RestTemplate restTemplate;
    private final HttpEntity<HttpHeaders> headerEntity;
    private final String altinnUrl;
    private final String altinnProxyUrl;
    private final String altinnProxyFallbackUrl;
    private final AltinnrettigheterProxyKlient klient;
    private final TokenUtils tokenUtils;
    private final Unleash unleash;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, RestTemplate restTemplate, TokenUtils tokenUtils, Unleash unleash) {
        this.restTemplate = restTemplate;
        this.altinnUrl = altinnConfig.getAltinnurl();
        this.altinnProxyUrl = altinnConfig.getProxyUrl();
        this.altinnProxyFallbackUrl = altinnConfig.getProxyFallbackUrl();
        this.tokenUtils = tokenUtils;
        this.unleash = unleash;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        this.headerEntity = new HttpEntity<>(headers);

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
        if (unleash.isEnabled("arbeidsgiver.ditt-nav-arbeidsgiver-api.bruk-altinn-proxy")) {
            Map<String, String> parametre = new ConcurrentHashMap<>();
            parametre.put("$filter", filterParamVerdi);
            log.info("Henter organisasjoner fra Altinn via proxy");
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
        }} else {
            String query = String.format("&$filter=%s", filterParamVerdi);
            log.info("Henter organisasjoner fra Altinn");
            return hentReporteesFraAltinn(query, fnr);
        }
    }

    public List<Role> hentRoller(String fnr, String orgnr) {
        String query = "&subject=" + fnr + "&reportee=" + orgnr;
        String url = altinnUrl + "authorization/roles?ForceEIAuthentication" + query;
        log.info("Henter roller fra Altinn");
        return getFromAltinn(new ParameterizedTypeReference<List<Role>>() {}, url, ALTINN_ROLE_PAGE_SIZE, headerEntity);
    }

    @Cacheable(ALTINN_TJENESTE_CACHE)
    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        if (unleash.isEnabled("arbeidsgiver.ditt-nav-arbeidsgiver-api.bruk-altinn-proxy")) {
            Map<String, String> parametre = new ConcurrentHashMap<>();
            parametre.put("serviceCode", serviceKode);
            parametre.put("serviceEdition", serviceEdition);
            log.info("Henter rettigheter fra Altinn via proxy");

            return getReporteesFromAltinnViaProxy(
                    tokenUtils.getSelvbetjeningTokenContext(),
                    new Subject(fnr),
                    parametre,
                    altinnProxyUrl,
                    ALTINN_ORG_PAGE_SIZE
            );
        } else {
            String query = "&serviceCode=" + serviceKode
                    + "&serviceEdition=" + serviceEdition;
            log.info("Henter rettigheter fra Altinn");
            return hentReporteesFraAltinn(query, fnr);
        }
    }


    private List<Organisasjon> hentReporteesFraAltinn(String query, String fnr) {
        String baseUrl;
        HttpEntity<HttpHeaders> headers;
        baseUrl = altinnUrl;
        headers = headerEntity;
        query += "&subject=" + fnr;
        String url = baseUrl + "reportees/?ForceEIAuthentication" + query;
        return getFromAltinn(new ParameterizedTypeReference<>() {}, url, ALTINN_ORG_PAGE_SIZE, headers);
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

    <T> List<T> getFromAltinn(ParameterizedTypeReference<List<T>> typeReference, String url, int pageSize, HttpEntity<HttpHeaders> headers) {

        Set<T> response = new HashSet<T>();
        int pageNumber = 0;
        boolean hasMore = true;
        while (hasMore) {
            pageNumber++;
            try {
                String urlWithPagesizeAndOffset = url + "&$top=" + pageSize + "&$skip=" + ((pageNumber - 1) * pageSize);
                ResponseEntity<List<T>> exchange = restTemplate.exchange(urlWithPagesizeAndOffset, HttpMethod.GET, headers, typeReference);
                List<T> currentResponseList = exchange.getBody();
                response.addAll(currentResponseList);
                hasMore = currentResponseList.size() >= pageSize;
            } catch (RestClientException exception) {
                log.error("Feil fra Altinn: Exception: " + exception.getMessage());
                if(exception.getMessage().contains("403")){
                    throw new TilgangskontrollException("bruker har ikke en aktiv altinn profil");
                }
                else{
                    throw new AltinnException("Feil fra Altinn", exception);
                }
            }
        }
        return new ArrayList<T>(response);
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
