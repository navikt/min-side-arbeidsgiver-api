package no.nav.arbeidsgiver.min_side.services.tokenExchange

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("labs")
@Component
class TokenExchangeClientStub : TokenExchangeClient {
    override fun exchange(subjectToken: String, audience: String): TokenXToken {
        return TokenXToken(
            access_token = "fake-access-token",
            token_type = "fake-token-type",
            issued_token_type = "fake-issued-token-type",
            expires_in = 3600,
        )
    }
}