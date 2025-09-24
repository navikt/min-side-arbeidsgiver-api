package no.nav.arbeidsgiver.min_side.maskinporten

import java.time.Duration
import java.time.Instant

class MaskinportenClientStub: MaskinportenClient {
    override suspend fun fetchNewAccessToken(): TokenResponseWrapper {
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

class MaskinportenTokenServiceStub: MaskinportenTokenService {
    override suspend fun currentAccessToken() = "fake.maskinporten.token"
    override suspend fun tokenRefreshingLoop() {
        TODO("Not yet implemented")
    }
}
