package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import lombok.Setter;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

@Slf4j
@ConfigurationProperties("aktorregister")
@Component
public class AktorClient {

    @Autowired
    private STSClient stsClient;

    @Setter
    private String aktorUrl;

    private final RestTemplate restTemplate;

    AktorClient(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }


    public String getAktorId(String fnr){

        String uriString = UriComponentsBuilder.fromHttpUrl(aktorUrl)
                .queryParam("identgruppe","AktoerId")
                .queryParam("gjeldende","true")
                .toUriString();

        HttpEntity<String> entity = getRequestEntity(fnr);

        try {
            ResponseEntity<AktorResponse> response = restTemplate.exchange(uriString, HttpMethod.GET, entity, AktorResponse.class);
            if(response.getStatusCode() != HttpStatus.OK){
                String message = "Kall mot aktørregister feiler med HTTP-" + response.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);
            }
            if(response.getBody().get(fnr).feilmelding != null){
                String message = "feilmelding på aktør: " + response.getBody().get(fnr).feilmelding;
                log.error(message);
                throw new RuntimeException(message);
            }
            return (response.getBody().get(fnr).identer.get(0).ident);
        }
        catch(HttpClientErrorException e){
            log.error("Feil ved oppslag i aktørtjenesten", e);
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<String> getRequestEntity(String fnr) {
        String appName= "srvditt-nav-arbeid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + stsClient.getToken().getAccess_token());
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", appName);
        headers.set("Nav-Personidenter", fnr);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }
}
