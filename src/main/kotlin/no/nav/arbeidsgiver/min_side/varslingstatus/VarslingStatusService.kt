package no.nav.arbeidsgiver.min_side.varslingstatus

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.isActiveAndNotTerminating
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class VarslingStatusService(
    private val altinnTilgangerService: AltinnTilgangerService,
    private val repository: VarslingStatusRepository,
) {

    val retention = 7.days

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

    suspend fun cleanup() {
        repository.slettVarslingStatuserEldreEnn(retention)
    }

    @Serializable
    data class VarslingStatusRequest(
        val virksomhetsnummer: String,
    )
}


suspend fun Application.startCleanupVarslingStatus(
    scope: CoroutineScope,
) {
    val varslingStatusService = dependencies.resolve<VarslingStatusService>()

    scope.launch {
        while (isActiveAndNotTerminating) {
            varslingStatusService.cleanup()
            delay(1.hours)
        }
    }
}