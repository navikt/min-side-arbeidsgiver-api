package no.nav.arbeidsgiver.min_side.services.tokenExchange;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenXProperties.*;

@Profile({"local", "dev-gcp","prod-gcp"})
@Component
public class TokenExchangeClientImpl implements TokenExchangeClient {

    private final RestTemplate restTemplate;
    private final TokenXProperties properties;
    private final ClientAssertionTokenFactory clientAssertionTokenFactory;


    TokenExchangeClientImpl(
            TokenXProperties properties,
            ClientAssertionTokenFactory clientAssertionTokenFactory,
            RestTemplate restTemplate
    ) {
        this.properties = properties;
        this.clientAssertionTokenFactory = clientAssertionTokenFactory;
        this.restTemplate = restTemplate;
    }

    @Override
    public TokenXToken exchange(String subjectToken, String audience) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(new LinkedMultiValueMap<>(Map.of(
                "grant_type", List.of(GRANT_TYPE),
                "client_assertion_type", List.of(CLIENT_ASSERTION_TYPE),
                "subject_token_type", List.of(SUBJECT_TOKEN_TYPE),
                "subject_token", List.of(subjectToken),
                "client_assertion", List.of(clientAssertionTokenFactory.getClientAssertion()),
                "audience", List.of(audience),
                "client_id", List.of(properties.clientId)
        )), headers);

        return restTemplate.postForEntity(properties.issuer, request, TokenXToken.class).getBody();
    }
}
