package no.nav.tag.dittNavArbeidsgiver.utils;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@ConfigurationProperties("aad")
public class AccesstokenClient {

    public void setAadAccessTokenURL(String aadAccessTokenURL) {
        this.aadAccessTokenURL = aadAccessTokenURL;

    }

    private String aadAccessTokenURL;
    @Setter
    private String azureClientSecret;
    @Autowired
    public AccesstokenClient( ){

    }

    public AadAccessToken hentAccessToken() {
        RestTemplate template = new RestTemplate();
        HttpEntity<MultiValueMap<String, String>> entity = getRequestEntity();
        try {
            ResponseEntity <AadAccessToken> response = template.exchange(aadAccessTokenURL, HttpMethod.POST, entity, AadAccessToken.class);
            return response.getBody();

        } catch (RestClientException exception) {
            log.error("Feil ved oppslag i STS", exception);
            throw exception;
        }
    }

    private HttpEntity<MultiValueMap<String, String>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_id", "1b1bf278-3c28-4003-a528-b595d800afb0");
        map.add("client_secret", azureClientSecret);
        map.add("resource", "3f567c84-4912-4acf-88ef-9f0dcfc2ae2b");

        return new HttpEntity<>(map, headers);
    }
}
