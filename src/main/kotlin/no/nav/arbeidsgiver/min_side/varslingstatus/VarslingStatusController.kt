package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDateTime

@RestController
class VarslingStatusController(
    private val altinnService: AltinnService,
    private val repository: VarslingStatusRepository,
) {

    @PostMapping("/api/varslingStatus/v1")
    fun getVarslingStatus(@RequestBody requestBody: VarslingStatusRequest): VarslingStatus {
        val harTilgang = altinnService.harOrganisasjon(requestBody.virksomhetsnummer)
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
