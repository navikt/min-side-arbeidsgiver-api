package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import java.time.Instant
import java.time.LocalDateTime

class VarslingStatusService(
    private val altinnService: AltinnService,
    private val repository: VarslingStatusRepository,
) {
    suspend fun getVarslingStatus(requestBody: VarslingStatusRequest, token: String): VarslingStatus {
        val harTilgang = altinnService.harOrganisasjon(requestBody.virksomhetsnummer, token)
        if (!harTilgang) {
            return VarslingStatus(
                status = Status.OK,
                varselTimestamp = LocalDateTime.now(),
                kvittertEventTimestamp = Instant.now(),
            )
        }

        return repository.varslingStatus(virksomhetsnummer = requestBody.virksomhetsnummer)
    }

    data class VarslingStatusRequest(
        val virksomhetsnummer: String,
    )
}
