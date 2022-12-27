package no.nav.arbeidsgiver.min_side.services.tokenExchange;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({"labs"})
@Component
public class TokenExchangeClientStub implements TokenExchangeClient {
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
