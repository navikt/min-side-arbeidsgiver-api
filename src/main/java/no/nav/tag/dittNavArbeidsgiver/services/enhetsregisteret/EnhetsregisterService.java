package no.nav.tag.dittNavArbeidsgiver.services.enhetsregisteret;

import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.BestaarAvOrganisasjonsledd;
import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.EnhetsRegisterOrg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EnhetsregisterService {
    @Value("${ereg.url}")
    private String eregUrl;
    private final RestTemplate restTemplate;
    private HttpEntity<String> requestEntity;

    public EnhetsregisterService( RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        this.requestEntity = getRequestEntity();
    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity<>(headers);
    }

    public EnhetsRegisterOrg hentOrgnaisasjonFraEnhetsregisteret(String orgnr){
        ResponseEntity <EnhetsRegisterOrg> response =  restTemplate.exchange(eregUrl, HttpMethod.GET,requestEntity,EnhetsRegisterOrg.class );
      return response.getBody();
    }
}
