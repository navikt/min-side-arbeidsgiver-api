package no.nav.arbeidsgiver.min_side.maskinporten


class MaskinportenTokenServiceStub: MaskinportenTokenService {
    override suspend fun currentAccessToken() = "fake.maskinporten.token"
    override suspend fun tokenRefreshingLoop() {
        TODO("Not yet implemented")
    }
}
