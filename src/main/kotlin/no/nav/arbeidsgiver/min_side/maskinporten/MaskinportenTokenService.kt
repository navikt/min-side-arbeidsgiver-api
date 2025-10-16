package no.nav.arbeidsgiver.min_side.maskinporten

interface MaskinportenTokenService {
    suspend fun currentAccessToken(): String
}

class MaskinportenTokenServiceImpl(
    private val maskinportenClient: MaskinportenClient,
) : MaskinportenTokenService {
    override suspend fun currentAccessToken() = maskinportenClient.fetchNewAccessToken()
}