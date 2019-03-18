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

@Slf4j
@Component
public class AltinnService {
    @Autowired
    private final AltinnConfig altinnConfig;

    @Autowired
    public AltinnService(AltinnConfig altinnConfig) {
        this.altinnConfig = altinnConfig;
    }

    public List<Organisasjon> hentOrganisasjoner(String fnr) {
        // TODO: Valider fnr med bekk validator

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnConfig.getAPIGwHeader());
        headers.set("APIKEY", altinnConfig.getAltinnHeader());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        String url = altinnConfig.getAltinnurl() + "/reportees/?ForceEIAuthentication=&subject=" + fnr;

        try {
            ResponseEntity<List<Organisasjon>> respons = restTemplate.exchange(url,
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<Organisasjon>>() {});
            return respons.getBody();

        } catch (RestClientException exception) {
            log.error("Feil fra Altinn. Exception: ", exception);
            throw new AltinnException("Feil fra Altinn");
        }
    }
}
