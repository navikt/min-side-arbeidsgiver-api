package no.nav.tag.dittNavArbeidsgiver.services.tokenExchange;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;


@Component
public class TokenExchangeClient {
    private String GRANT_TYPE= "urn:ietf:params:oauth:grant-type:token-exchange";
    private String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private String subjectToken;
    private String clientAssertion;
    private String audience;
    private String clientId;
    private String tokendingsUrl;

    ClientAssertionTokenFactory clientAssertionTokenFactory;
    RestTemplate restTemplate;

    TokenExchangeClient(
            ClientAssertionTokenFactory clientAssertionTokenFactory,
            @Value("${tokenX.clientId}") String clientId,
            @Value("${tokenX.audience}") String audience,
            @Value("${tokenX.tokendingsUrl}") String tokendingsUrl,
            RestTemplate restTemplate
    ){
        this.clientAssertionTokenFactory = clientAssertionTokenFactory;
        this.clientId = clientId;
        this.audience = audience;
        this.restTemplate = restTemplate;
        this.tokendingsUrl = tokendingsUrl;

    }

    public String exchangeToken(String subjectToken){
        return restTemplate.postForEntity(tokendingsUrl, getRequestEntity(subjectToken), String.class).getBody();
    }



    private HttpEntity<MultiValueMap<String, String>> getRequestEntity(String subjectToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", GRANT_TYPE);
        map.add("client_assertion_type", CLIENT_ASSERTION_TYPE);
        map.add("subject_token_type", SUBJECT_TOKEN_TYPE);
        map.add("subject_token", subjectToken);
        map.add("client_assertion", clientAssertionTokenFactory.getClientAssertion());
        map.add("audience", audience);
        map.add("client_id", clientId);

        return new HttpEntity<>(map, headers);
    }


}
