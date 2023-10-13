package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDateTime

@RestController
class VarslingStatusController(
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    private val altinnService: AltinnService,
    private val repository: VarslingStatusRepository,
) {

    @PostMapping("/api/varslingStatus/v1")
    fun getVarslingStatus(@RequestBody requestBody: VarslingStatusRequest): VarslingStatus {
        val virksomhetsnummer = requestBody.virksomhetsnummer
        val harTilgang = altinnService.hentOrganisasjoner(authenticatedUserHolder.fnr)
            .any { it.organizationNumber == virksomhetsnummer }

        if (!harTilgang) {
            return VarslingStatus(
                status = Status.OK,
                varselTimestamp = LocalDateTime.now(),
                kvittertEventTimestamp = Instant.now(),
            )
        }

        return repository.varslingStatus(virksomhetsnummer = virksomhetsnummer)
    }

    data class VarslingStatusRequest(
        val virksomhetsnummer: String,
    )
}
