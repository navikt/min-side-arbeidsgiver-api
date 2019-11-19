package no.nav.tag.dittNavArbeidsgiver.services.sts;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.DittNavArbeidsgiverApplication;

import static no.nav.tag.dittNavArbeidsgiver.services.sts.StsCacheConfig.STS_CACHE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class STSClient {

    @Value("${sts.stsPass}")private String stsPass;
    @Value("${sts.stsUrl}")private String stsUrl;

    @Cacheable(STS_CACHE)
    public STStoken getToken() {
        try {
            ResponseEntity<STStoken> response = buildUriAndExecuteRequest();
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

    private ResponseEntity<STStoken> buildUriAndExecuteRequest(){
        RestTemplate basicAuthRestTemplate = new RestTemplateBuilder().basicAuthentication(DittNavArbeidsgiverApplication.APP_NAME, stsPass).build();
        String uriString = UriComponentsBuilder.fromHttpUrl(stsUrl)
                .queryParam("grant_type","client_credentials")
                .queryParam("scope","openid")
                .toUriString();
        HttpEntity<String> entity = getRequestEntity();
        return basicAuthRestTemplate.exchange(uriString, HttpMethod.GET,entity,STStoken.class);
    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(headers);
    }

    @CacheEvict(STS_CACHE)
    public void evict() {
    }

}
