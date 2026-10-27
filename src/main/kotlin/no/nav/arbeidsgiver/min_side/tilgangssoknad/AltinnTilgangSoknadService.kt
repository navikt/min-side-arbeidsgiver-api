package no.nav.arbeidsgiver.min_side.tilgangssoknad

import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.infrastruktur.logger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import java.util.*

class AltinnTilgangSoknadService(
    private val altinnTilgangssoknadClient: AltinnTilgangssoknadClient,
    private val delegationRequestRepository: DelegationRequestRepository,
    private val altinnTilgangerService: AltinnTilgangerService,
) {
    private val log = logger()

    /**
     * Returnerer null dersom bruker ikke har tilgang til organisasjonen i [CreateDelegationRequest.to].
     */
    suspend fun opprettDelegationRequest(
        fnr: String,
        token: String,
        request: CreateDelegationRequest,
    ): DelegationRequestResponse? {
        validerResource(request.resource?.referenceId)
        val orgnr = validerTo(request.to)

        if (!altinnTilgangerService.harOrganisasjon(orgnr, token)) {
            return null
        }

        val response = altinnTilgangssoknadClient.opprettDelegationRequest(fnr, request)

        val id = response.id?.let(UUID::fromString)
        val responseOrgnr = response.from?.organizationIdentifier ?: orgnr
        val resourceReferenceId = response.resource?.referenceId
            ?: request.resource?.referenceId
        val status = response.status?.name

        if (id != null && resourceReferenceId != null && status != null) {
            delegationRequestRepository.lagre(
                id = id,
                fnr = fnr,
                orgnr = responseOrgnr,
                resourceReferenceId = resourceReferenceId,
                status = status,
                detailsLink = response.links?.detailsLink,
                lastResponseJson = defaultJson.encodeToString(DelegationRequestResponse.serializer(), response),
            )
        } else {
            log.warn(
                "Kunne ikke persistere delegation request: id={}, orgnr={}, resource={}, status={}",
                id, responseOrgnr, resourceReferenceId, status
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

private val ORG_URN = Regex("""^urn:altinn:organization:identifier-no:(\d{9})$""")

private fun validerTo(to: String?): String {
    val match = to?.let(ORG_URN::matchEntire)
    require(match != null) { "to må være en organisasjons-urn" }
    return match.groupValues[1]
}

private val TERMINAL_STATUSES = setOf(
    DelegationRequestStatus.Approved.name,
    DelegationRequestStatus.Rejected.name,
    DelegationRequestStatus.Withdrawn.name,
)
