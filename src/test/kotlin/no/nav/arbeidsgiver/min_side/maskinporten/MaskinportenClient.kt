package no.nav.arbeidsgiver.min_side.maskinporten

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration

@Component
@Profile("local", "test")
class MaskinportenClientStub: MaskinportenClient {
    override fun fetchNewAccessToken(): TokenResponseWrapper {
        return TokenResponseWrapper(
            requestedAt = Instant.now(),
            tokenResponse = TokenResponse(
                accessToken = "",
                tokenType = "",
                expiresInSeconds = Duration.ofHours(1).toSeconds(),
                scope = "",
            )
        )
    }
}

