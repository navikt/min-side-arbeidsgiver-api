package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AltinnService {

    private final AltinnConfig altinnConfig;

    private final RestTemplate restTemplate;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig, RestTemplate restTemplate) {
        this.altinnConfig = altinnConfig;
        this.restTemplate = restTemplate;
    }

    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        String query = "&subject=" + fnr;
        ResponseEntity<List<Organisasjon>> respons = getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},query);
        return filtrerPaBusiness(respons.getBody());
    }

    public List<Organisasjon> filtrerPaBusiness(List<Organisasjon> ufiltrert) {
        return ufiltrert.stream()
                .filter(org -> "Business".equals(org.getType()))
                .collect(Collectors.toList());
    }

    private HttpEntity<String>  getheaderEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }

    private <T> ResponseEntity<List<T>> getFromAltinn(ParameterizedTypeReference<List<T>> typeReference, String query){
        String url = altinnConfig.getAltinnurl() + "/reportees/?ForceEIAuthentication" + query;
        HttpEntity<String> headers = getheaderEntity();
        try {
            ResponseEntity<List<T>> respons = restTemplate.exchange(url,
                    HttpMethod.GET, headers, typeReference);
            return respons;

        } catch (RestClientException exception) {
            log.error("Feil fra Altinn. Exception: ", exception);
            throw new AltinnException("Feil fra Altinn", exception);
        }

    }

}
