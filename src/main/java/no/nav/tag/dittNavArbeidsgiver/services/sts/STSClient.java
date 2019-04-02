package no.nav.tag.dittNavArbeidsgiver.services.sts;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@ConfigurationProperties("sts")
@Component
public class STSClient {

    @Setter
    private String stsPass;
    @Setter
    private String stsUrl;
    public STStoken getToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        RestTemplate basicAuthRestTemplate = new RestTemplateBuilder().basicAuthentication("srvditt-nav-arbeid",stsPass).build();
        String uriString = UriComponentsBuilder.fromHttpUrl(stsUrl)
                .queryParam("grant_type","client_credentials")
                .queryParam("scope","openid")
                .toUriString();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<STStoken> response = basicAuthRestTemplate.exchange(uriString, HttpMethod.GET,entity,STStoken.class);
            if(response.getStatusCode() != HttpStatus.OK){
                String message = "Kall mot STS feiler med HTTP-" + response.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);

            }
            return (response.getBody());
        }
        catch(HttpClientErrorException e){
            log.error("Feil ved oppslag i STS", e);
            throw new RuntimeException(e);
        }
    }

}
