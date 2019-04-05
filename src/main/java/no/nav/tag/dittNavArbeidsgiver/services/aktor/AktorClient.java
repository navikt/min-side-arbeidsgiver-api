package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
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
        } catch(HttpClientErrorException e ) {
            throw new AktorException("Feil ved oppslag i aktørtjenesten", e);
        }
    }

    private ResponseEntity<AktorResponse> utforKallTilAktorregister(String fnr) {
        String uriString = UriComponentsBuilder.fromHttpUrl(aktorUrl)
                .queryParam("identgruppe","AktoerId")
                .queryParam("gjeldende","true")
                .toUriString();
        HttpEntity<String> entity = getRequestEntity(fnr);

        return restTemplate.exchange(uriString, HttpMethod.GET, entity, AktorResponse.class);
    }

    private HttpEntity<String> getRequestEntity(String fnr) {
        String appName= "srvditt-nav-arbeid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + stsClient.getToken().getAccess_token());
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", appName);
        headers.set("Nav-Personidenter", fnr);
        return new HttpEntity<>(headers);
    }
}
