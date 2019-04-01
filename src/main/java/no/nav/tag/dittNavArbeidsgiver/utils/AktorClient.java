package no.nav.tag.dittNavArbeidsgiver.utils;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private STSClient stsClient;

    public String getAktorId(String fnr){
        String appName= "srvditt-nav-arbeid";
        String aktorURL="https://app-q1.adeo.no/aktoerregister/api/v1/identer";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + stsClient.getToken().access_token);
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", appName);
        headers.set("Nav-Personidenter", fnr);

        String uriString = UriComponentsBuilder.fromHttpUrl(aktorURL)
                .queryParam("identgruppe","AktoerId")
                .queryParam("gjeldende",true)
                .toUriString();
        RestTemplate restTemplate= new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<AktorResponse> response = restTemplate.exchange(uriString, HttpMethod.GET, entity, AktorResponse.class);
            if(response.getStatusCode() != HttpStatus.OK){
                String message = "Kall mot aktørregister feiler med HTTP-" + response.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);

            }
            if(response.getBody().getAktorer().get(fnr).Feilmelding != null){
                String message = "feilmelding på aktør: " + response.getBody().getAktorer().get(fnr).Feilmelding;
                log.error(message);
                throw new RuntimeException(message);
            }

            return (response.getBody().aktorer.get(fnr).identer.get(0).ident);
        }
        catch(HttpClientErrorException e){
            log.error("Feil ved oppslag i aktørtjenesten", e);
            throw new RuntimeException(e);
        }




    }


}
