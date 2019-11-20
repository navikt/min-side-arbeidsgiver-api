package no.nav.tag.dittNavArbeidsgiver.services.aad;


import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static no.nav.tag.dittNavArbeidsgiver.services.aad.AadCacheConfig.AAD_CACHE;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@Setter
@ConfigurationProperties("aad")
@RequiredArgsConstructor
public class AccesstokenClient {

    private String aadAccessTokenURL;
    private String clientid;
    private String azureClientSecret;
    private String scope;
    
    private final RestTemplate template;

    @Cacheable(AAD_CACHE)
    public AadAccessToken hentAccessToken() {
        try {
            ResponseEntity <AadAccessToken> response = template.exchange(aadAccessTokenURL, HttpMethod.POST, getRequestEntity(), AadAccessToken.class);
            return response.getBody();

        } catch (RestClientException exception) {
            log.error("Feil ved oppslag i AAD", exception);
            throw exception;
        }
    }

    private HttpEntity<MultiValueMap<String, String>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_id", clientid);
        map.add("client_secret", azureClientSecret);
        map.add("resource", scope);

        return new HttpEntity<>(map, headers);
    }
    
    @CacheEvict(AAD_CACHE)
    public void evict() {
    }

}
