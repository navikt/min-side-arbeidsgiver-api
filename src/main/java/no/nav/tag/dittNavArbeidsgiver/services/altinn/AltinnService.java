package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        String query = "&subject=" + fnr 
                + "&$top=" + ALTINN_PAGE_SIZE 
                + "&$filter=(Type+eq+'Bedrift'+or+Type+eq+'Business'+or+Type+eq+'Enterprise'+or+Type+eq+'Foretak')+and+Status+eq+'Active'";
        String url = altinnConfig.getAltinnurl() + "reportees/?ForceEIAuthentication" + query;
        ResponseEntity<List<Organisasjon>> respons = getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},url);
        log.info("Henter organisasjoner fra Altinn");
        return respons.getBody();
    }

    public List<Role> hentRoller(String fnr, String orgnr) {
        String query = "&subject=" + fnr + "&reportee=" + orgnr;
        String url = altinnConfig.getAltinnurl() + "authorization/roles?ForceEIAuthentication" + query;
        ResponseEntity<List<Role>> respons = getFromAltinn(new ParameterizedTypeReference<List<Role>>() {},url);
        log.info("Henter roller fra Altinn");
        return respons.getBody();
    }

    public List<Organisasjon> hentOrganisasjonerBasertPaRettigheter(String fnr, String serviceKode, String serviceEdition) {
        String query = "&subject=" + fnr 
                + "&serviceCode=" + serviceKode 
                + "&serviceEdition=" + serviceEdition 
                + "&$top=" + ALTINN_PAGE_SIZE;
        String url = altinnConfig.getAltinnurl() + "reportees/?ForceEIAuthentication" + query;
        ResponseEntity<List<Organisasjon>> respons = getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},url);
        log.info("Henter rettigheter fra Altinn");
        return respons.getBody();
    }

    private HttpEntity<String>  getHeaderEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        return new HttpEntity<>(headers);
    }

    private <T> ResponseEntity<List<T>> getFromAltinn(ParameterizedTypeReference<List<T>> typeReference, String url){
        HttpEntity<String> headers = getHeaderEntity();
        try {
           return restTemplate.exchange(url,
                    HttpMethod.GET, headers, typeReference);
        } catch (RestClientException exception) {
            log.error("Feil fra Altinn med spørring: " + url + " Exception: " + exception.getMessage());
            throw new AltinnException("Feil fra Altinn", exception);
        }
    }

}
