package no.nav.arbeidsgiver.min_side.services.tokenExchange;

public interface TokenExchangeClient {
    TokenXToken exchange(String subjectToken, String audience);
}
