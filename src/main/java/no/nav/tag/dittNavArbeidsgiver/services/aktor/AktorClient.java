package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import no.nav.tag.dittNavArbeidsgiver.DittNavArbeidsgiverApplication;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
public class AktorClient {

    @Value("${aktorregister.aktorUrl}") private String aktorUrl;
    private final RestTemplate restTemplate;
    private final STSClient stsClient;

    AktorClient(RestTemplate restTemplate,STSClient stsClient){
        this.restTemplate = restTemplate;
        this.stsClient = stsClient;
    }

    public String getAktorId(String fnr){
        try {
            ResponseEntity<AktorResponse> response = utforKallTilAktorregister(fnr);
            return response.getBody().get(fnr).identer.get(0).ident;
        } catch(NullPointerException e ) {
            throw new AktorException("Feil ved oppslag i aktørtjenesten", e);
        }
    }

    private ResponseEntity<AktorResponse> utforKallTilAktorregister(String fnr) {
        String uriString = UriComponentsBuilder.fromHttpUrl(aktorUrl)
                .queryParam("identgruppe","AktoerId")
                .queryParam("gjeldende","true")
                .toUriString();
        ResponseEntity<AktorResponse> response = null; 
        try {
            response = restTemplate.exchange(uriString, HttpMethod.GET, getRequestEntity(fnr), AktorResponse.class);
        } catch (RestClientException e) {
            stsClient.evict();
            response = restTemplate.exchange(uriString, HttpMethod.GET, getRequestEntity(fnr), AktorResponse.class);
        }
        return response;
    }

    private HttpEntity<String> getRequestEntity(String fnr) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + stsClient.getToken().getAccess_token());
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", DittNavArbeidsgiverApplication.APP_NAME);
        headers.set("Nav-Personidenter", fnr);
        return new HttpEntity<>(headers);
    }
}
