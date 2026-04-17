package no.nav.arbeidsgiver.min_side.tilgangssoknad

import no.nav.arbeidsgiver.min_side.infrastruktur.logger
import java.util.*

class AltinnTilgangSoknadService(
    private val altinnTilgangssoknadClient: AltinnTilgangssoknadClient,
    private val delegationRequestRepository: DelegationRequestRepository,
) {
    private val log = logger()

    suspend fun opprettDelegationRequest(
        fnr: String,
        request: CreateDelegationRequest,
    ): DelegationRequestResponse {
        validerResource(request.resource?.referenceId)

        val response = altinnTilgangssoknadClient.opprettDelegationRequest(fnr, request)

        val id = response.id?.let(UUID::fromString)
        val orgnr = response.to?.organizationIdentifier
            ?: request.to?.extractOrgnr()
        val resourceReferenceId = response.resource?.referenceId
            ?: request.resource?.referenceId
        val status = response.status?.name

        if (id != null && orgnr != null && resourceReferenceId != null && status != null) {
            delegationRequestRepository.lagre(id, fnr, orgnr, resourceReferenceId, status)
        } else {
            log.warn(
                "Kunne ikke persistere delegation request: id={}, orgnr={}, resource={}, status={}",
                id, orgnr, resourceReferenceId, status
            )
        }

        return response
    }

    suspend fun mineDelegationRequests(fnr: String): List<DelegationRequestRow> {
        val rows = delegationRequestRepository.hentForBruker(fnr)
        val refreshed = rows.map { row ->
            if (row.status in TERMINAL_STATUSES) {
                row
            } else {
                try {
                    val newStatus = altinnTilgangssoknadClient
                        .hentDelegationRequestStatus(row.id)
                        .name
                    if (newStatus != row.status) {
                        delegationRequestRepository.oppdaterStatus(UUID.fromString(row.id), newStatus)
                        row.copy(status = newStatus)
                    } else {
                        row
                    }
                } catch (e: Exception) {
                    log.warn("Kunne ikke oppdatere status for delegation request {}: {}", row.id, e.message)
                    row
                }
            }
        }
        return refreshed
    }
}

private fun validerResource(referenceId: String?) {
    require(referenceId != null) { "resource.referenceId mangler" }
    require(referenceId.startsWith(AltinnTilgangssoknadClient.NAV_RESOURCE_PREFIX)) {
        "resource.referenceId må begynne med '${AltinnTilgangssoknadClient.NAV_RESOURCE_PREFIX}', fikk '$referenceId'"
    }
    // evt ressurser som ikke skal kunne bes om? endre_kontonummer feks
}

private val TERMINAL_STATUSES = setOf(
    DelegationRequestStatus.Approved.name,
    DelegationRequestStatus.Rejected.name,
    DelegationRequestStatus.Withdrawn.name,
)

private const val ORG_URN_PREFIX = "urn:altinn:organization:identifier-no:"

private fun String.extractOrgnr(): String? =
    if (startsWith(ORG_URN_PREFIX)) removePrefix(ORG_URN_PREFIX) else null
