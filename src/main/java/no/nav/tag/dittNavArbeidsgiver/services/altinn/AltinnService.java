package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
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

    private static final int ALTINN_PAGE_SIZE = 500;

    private final AltinnConfig altinnConfig;

    private final RestTemplate restTemplate;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, RestTemplate restTemplate) {
        this.altinnConfig = altinnConfig;
        this.restTemplate = restTemplate;
    }

    @Cacheable(ALTINN_CACHE)
    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        String query = "&subject=" + fnr 
                + "&$filter=(Type+eq+'Bedrift'+or+Type+eq+'Business'+or+Type+eq+'Enterprise'+or+Type+eq+'Foretak')+and+Status+eq+'Active'";
        String url = altinnConfig.getAltinnurl() + "reportees/?ForceEIAuthentication" + query;
        log.info("Henter organisasjoner fra Altinn");
        return getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},url, ALTINN_PAGE_SIZE);
    }

    public List<Role> hentRoller(String fnr, String orgnr) {
        String query = "&subject=" + fnr + "&reportee=" + orgnr;
        String url = altinnConfig.getAltinnurl() + "authorization/roles?ForceEIAuthentication" + query;
        log.info("Henter roller fra Altinn");
        return getFromAltinn(new ParameterizedTypeReference<List<Role>>() {},url, ALTINN_PAGE_SIZE);
    }

    @Cacheable(ALTINN_TJENESTE_CACHE)
    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        String query = "&subject=" + fnr 
                + "&serviceCode=" + serviceKode 
                + "&serviceEdition=" + serviceEdition; 
        String url = altinnConfig.getAltinnurl() + "reportees/?ForceEIAuthentication" + query;
        log.info("Henter rettigheter fra Altinn");
        return getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},url, ALTINN_PAGE_SIZE);
    }

    private HttpEntity<String>  getHeaderEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        return new HttpEntity<>(headers);
    }

    private <T> List<T> getFromAltinn(ParameterizedTypeReference<List<T>> typeReference, String url, int pageSize) {

        Set<T> response = new HashSet<T>();
        HttpEntity<String> headers = getHeaderEntity();
        int pageNumber = 0;
        boolean hasMore = true;
        while (hasMore) {
            pageNumber++;
            try {
                String urlWithPagesizeAndOffset = url + "&$top=" + pageSize + "&$skip=" + ((pageNumber-1) * pageSize);
                List<T> currentResponseList = restTemplate.exchange(urlWithPagesizeAndOffset, HttpMethod.GET, headers, typeReference).getBody();
                response.addAll(currentResponseList);
                hasMore = currentResponseList.size() >= pageSize;
            } catch (RestClientException exception) {
                log.error("Feil fra Altinn med spørring: " + url + " Exception: " + exception.getMessage());
                throw new AltinnException("Feil fra Altinn", exception);
            }
        }
        return new ArrayList<T>(response);
    }

}
