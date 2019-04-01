package no.nav.tag.dittNavArbeidsgiver.utils;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties("sts")
@Component
public class STSClient {

    @Setter
    private String stsPass;
    @Setter
    private String stsUrl;
    STStoken getToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        RestTemplate basicAuthRestTemplate = new RestTemplateBuilder().basicAuthentication("srvditt-nav-arbeid",stsPass).build();
        String uriString = UriComponentsBuilder.fromHttpUrl(stsUrl)
                .queryParam("grant_type","client_credentials")
                .queryParam("scope","openid")
                .toUriString();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<STStoken> result = basicAuthRestTemplate.exchange(uriString, HttpMethod.GET,entity,STStoken.class);


        return (result.getBody());
    }

}
