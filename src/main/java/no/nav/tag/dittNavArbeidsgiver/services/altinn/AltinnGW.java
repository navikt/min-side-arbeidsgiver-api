package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class AltinnGW {
    private final AltinnConfig altinnEnvConf;

    @Autowired
    public AltinnGW(AltinnConfig altinnEnvConf) {
        this.altinnEnvConf = altinnEnvConf;
    }

    public List<Organization> getOrganizations(String pnr){
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NAV-APIKEY", altinnEnvConf.getAPIGwHeader());
        headers.set("APIKEY", altinnEnvConf.getAltinnHeader());
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        RestTemplate restTemplate = new RestTemplate();
        String url = altinnEnvConf.getAltinnurl() + "/reportees/?ForceEIAuthentication=&subject=14044500761";
        ResponseEntity <List<Organization>> response = restTemplate.exchange(url,
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<Organization>>() {
                });
        return response.getBody();
    }

}
