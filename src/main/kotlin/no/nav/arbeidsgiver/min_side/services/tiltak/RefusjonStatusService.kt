package no.nav.arbeidsgiver.min_side.services.tiltak

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService


class RefusjonStatusService(
    private val altinnService: AltinnService,
    private val refusjonStatusRepository: RefusjonStatusRepository,
) {

    suspend fun statusoversikt(authenticatedUserHolder: AuthenticatedUserHolder): List<Statusoversikt> {
        val orgnr = altinnService.hentAltinnTilganger(authenticatedUserHolder)
            .tilgangTilOrgNr["$TJENESTEKODE:$TJENESTEVERSJON"] ?: emptySet()

        return refusjonStatusRepository
            .statusoversikt(orgnr)
            .map(Statusoversikt.Companion::from)
    }

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
        const val TJENESTEKODE = "4936"
        const val TJENESTEVERSJON = "1"
    }
}