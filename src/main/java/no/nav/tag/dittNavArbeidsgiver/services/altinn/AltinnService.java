package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.finn.unleash.Unleash;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final TokenUtils tokenUtils;
    private final Unleash unleash;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, RestTemplate restTemplate, TokenUtils tokenUtils, Unleash unleash) {
        this.restTemplate = restTemplate;
        this.altinnUrl = altinnConfig.getAltinnurl();
        this.altinnProxyUrl = altinnConfig.getProxyUrl();
        this.tokenUtils = tokenUtils;
        this.unleash = unleash;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        this.headerEntity = new HttpEntity<>(headers);
    }

    @Cacheable(ALTINN_CACHE)
    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        String query = "&subject=" + fnr
                + "&$filter=Type+ne+'Person'+and+Status+eq+'Active'";
        log.info("Henter organisasjoner fra Altinn");
        return hentReporteesFraAltinn(query);
    }

    public List<Role> hentRoller(String fnr, String orgnr) {
        String query = "&subject=" + fnr + "&reportee=" + orgnr;
        String url = altinnUrl + "authorization/roles?ForceEIAuthentication" + query;
        log.info("Henter roller fra Altinn");
        return getFromAltinn(new ParameterizedTypeReference<List<Role>>() {}, url, ALTINN_ROLE_PAGE_SIZE, headerEntity);
    }

    @Cacheable(ALTINN_TJENESTE_CACHE)
    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        String query = "&subject=" + fnr
                + "&serviceCode=" + serviceKode
                + "&serviceEdition=" + serviceEdition;
        log.info("Henter rettigheter fra Altinn");
        return hentReporteesFraAltinn(query);
    }

    private List<Organisasjon> hentReporteesFraAltinn(String query) {
        String baseUrl;
        HttpEntity<HttpHeaders> headers;

        if (unleash.isEnabled("arbeidsgiver.ditt-nav-arbeidsgiver-api.bruk-altinn-proxy")) {
            baseUrl = altinnProxyUrl;
            headers = getAuthHeadersForInnloggetBruker();
        } else {
            baseUrl = altinnUrl;
            headers = headerEntity;
        }

        String url = baseUrl + "reportees/?ForceEIAuthentication" + query;

        return getFromAltinn(new ParameterizedTypeReference<>() {}, url, ALTINN_ORG_PAGE_SIZE, headers);
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
                log.error("Feil fra Altinn med spørring: " + url + " Exception: " + exception.getMessage());
                throw new AltinnException("Feil fra Altinn", exception);
            }
        }
        return new ArrayList<T>(response);
    }

    private HttpEntity<HttpHeaders> getAuthHeadersForInnloggetBruker() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUtils.getTokenForInnloggetBruker());
        return new HttpEntity<>(headers);
    }

}
