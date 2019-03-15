package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class AltinnGW {
    private final AltinnConfig altinnConfig;

    @Autowired
    public AltinnGW(AltinnConfig altinnConfig) {
        this.altinnConfig = altinnConfig;
    }

    public List<Organization> hentOrganisasjoner(String fnr) {
        // TODO: Valider fnr med bekk validator

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        String url = altinnConfig.getAltinnurl() + "/reportees/?ForceEIAuthentication=&subject=" + fnr;

        try {
            ResponseEntity<List<Organization>> respons = restTemplate.exchange(url,
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<Organization>>() {});
            return respons.getBody();

        } catch (RestClientException exception) {
            log.error("Feil fra Altinn. Exception: ", exception);
            throw new AltinnException("Feil fra Altinn");
        }
    }
}
