package no.nav.arbeidsgiver.min_side.services.tiltak

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.RESSURS_ID

interface RefusjonStatusService {
    suspend fun statusoversikt(token: String): List<Statusoversikt>

    @Serializable
    data class Statusoversikt(
        val virksomhetsnummer: String,
        val statusoversikt: Map<String, Int>,
    ) {
        val tilgang: Boolean = statusoversikt.values.any { it > 0 }

        companion object {
            fun from(statusoversikt: RefusjonStatusRepository.Statusoversikt) =
                Statusoversikt(
                    statusoversikt.virksomhetsnummer,
                    statusoversikt.statusoversikt
                )
        }
    }

    companion object {
        const val RESSURS_ID = "nav_tiltak_tiltaksrefusjon"
    }
}

class RefusjonStatusServiceImpl(
    private val altinnTilgangerService: AltinnTilgangerService,
    private val refusjonStatusRepository: RefusjonStatusRepository,
) : RefusjonStatusService {

    override suspend fun statusoversikt(token: String): List<RefusjonStatusService.Statusoversikt> {
        val orgnr = altinnTilgangerService.hentAltinnTilganger(token).tilgangTilOrgNr[RESSURS_ID] ?: emptySet()

        return refusjonStatusRepository
            .statusoversikt(orgnr)
            .map(RefusjonStatusService.Statusoversikt.Companion::from)
    }
}