package no.nav.tag.dittNavArbeidsgiver.utils;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
public class AktorClient {
    @Setter
    private String stsPass;
    @Setter
    private String Url;

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
        ResponseEntity<AktorResponse> result = restTemplate.exchange(uriString, HttpMethod.GET,entity,AktorResponse.class);


        return (result.getBody().aktorer.get(fnr).identer.get(0).ident);


    }


}
