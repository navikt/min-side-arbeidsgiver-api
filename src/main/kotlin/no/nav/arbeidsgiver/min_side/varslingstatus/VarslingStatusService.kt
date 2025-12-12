package no.nav.arbeidsgiver.min_side.varslingstatus

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import java.time.Instant
import java.time.LocalDateTime

class VarslingStatusService(
    private val altinnTilgangerService: AltinnTilgangerService,
    private val repository: VarslingStatusRepository,
) {
    suspend fun getVarslingStatus(requestBody: VarslingStatusRequest, token: String): VarslingStatus {
        val harTilgang = altinnTilgangerService.harOrganisasjon(requestBody.virksomhetsnummer, token)
        if (!harTilgang) {
            return VarslingStatus(
                status = Status.OK,
                varselTimestamp = LocalDateTime.now(),
                kvittertEventTimestamp = Instant.now(),
            )
        }
        val result = repository.varslingStatus(virksomhetsnummer = requestBody.virksomhetsnummer)
        return result
    }

    @Serializable
    data class VarslingStatusRequest(
        val virksomhetsnummer: String,
    )
}
