package no.nav.tag.dittNavArbeidsgiver.services.sts;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.DittNavArbeidsgiverApplication;

import static no.nav.tag.dittNavArbeidsgiver.services.sts.StsCacheConfig.STS_CACHE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@Profile({"dev", "prod", "local"})
public class STSClient {

    private RestTemplate restTemplate;
    private HttpEntity<String> requestEntity;
    private String uriString;

    @Autowired
    public STSClient(@Value("${sts.stsPass}") String stsPass, 
            @Value("${sts.stsUrl}") String stsUrl, 
            RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.requestEntity = getRequestEntity(stsPass);
        this.uriString = buildUriString(stsUrl);
    }

    private HttpEntity<String> getRequestEntity(String stsPass) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(DittNavArbeidsgiverApplication.APP_NAME, stsPass);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(headers);
    }

    private String buildUriString(String stsUrl) {
        return UriComponentsBuilder.fromHttpUrl(stsUrl)
                .queryParam("grant_type","client_credentials")
                .queryParam("scope","openid")
                .toUriString();
    }

    @Cacheable(STS_CACHE)
    public STStoken getToken() {
        try {
            ResponseEntity<STStoken> response = restTemplate.exchange(uriString, HttpMethod.GET, requestEntity, STStoken.class);
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

    @CacheEvict(STS_CACHE)
    public void evict() {
    }

}
