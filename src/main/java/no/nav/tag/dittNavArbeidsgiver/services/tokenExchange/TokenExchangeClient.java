package no.nav.tag.dittNavArbeidsgiver.services.tokenExchange;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Profile({"local","dev-gcp"})
@Component
public class TokenExchangeClient {
    static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    static final String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";

    final String audience;
    final String clientId;
    final String tokendingsUrl;
    final RestTemplate restTemplate;
    final ClientAssertionTokenFactory clientAssertionTokenFactory;

    TokenExchangeClient(
            ClientAssertionTokenFactory clientAssertionTokenFactory,
            @Value("${tokenX.clientId}") String clientId,
            @Value("${tokenX.audience}") String audience,
            @Value("${tokenX.tokendingsUrl}") String tokendingsUrl,
            RestTemplate restTemplate
    ) {
        this.clientAssertionTokenFactory = clientAssertionTokenFactory;
        this.clientId = clientId;
        this.audience = audience;
        this.restTemplate = restTemplate;
        this.tokendingsUrl = tokendingsUrl;

    }

    public String exchangeToken(String subjectToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(new LinkedMultiValueMap<>(Map.of(
                "grant_type", List.of(GRANT_TYPE),
                "client_assertion_type", List.of(CLIENT_ASSERTION_TYPE),
                "subject_token_type", List.of(SUBJECT_TOKEN_TYPE),
                "subject_token", List.of(subjectToken),
                "client_assertion", List.of(clientAssertionTokenFactory.getClientAssertion()),
                "audience", List.of(audience),
                "client_id", List.of(clientId)
        )), headers);

        return restTemplate.postForEntity(tokendingsUrl, request, String.class).getBody();
    }

}
