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

@Profile({"labs"})
@Component
public class TokenExchangeClientStub implements TokenExchangeClient {

    final RestTemplate restTemplate;
    final TokenXProperties properties;
    final ClientAssertionTokenFactory clientAssertionTokenFactory;

    TokenExchangeClientStub(
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
        var tokenXToken = new TokenXToken();
        tokenXToken.access_token = "fake-access-token";
        tokenXToken.token_type = "fake-token-type";
        tokenXToken.issued_token_type = "fake-issued-token-type";
        tokenXToken.expires_in = 3600;
        return tokenXToken;
    }
}
