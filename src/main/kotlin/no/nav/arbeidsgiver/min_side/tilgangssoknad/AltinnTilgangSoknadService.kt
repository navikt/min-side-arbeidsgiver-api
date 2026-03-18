package no.nav.arbeidsgiver.min_side.tilgangssoknad

class AltinnTilgangSoknadService(
    private val altinnTilgangssoknadClient: AltinnTilgangssoknadClient,
) {
    suspend fun opprettDelegationRequest(token: String, request: CreateDelegationRequest): DelegationRequestResponse {
        return altinnTilgangssoknadClient.opprettDelegationRequest(token, request)
    }

    suspend fun hentDelegationRequestStatus(token: String, id: String): DelegationRequestStatus {
        return altinnTilgangssoknadClient.hentDelegationRequestStatus(token, id)
    }
}