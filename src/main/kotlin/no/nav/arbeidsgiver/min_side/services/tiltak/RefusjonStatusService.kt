package no.nav.arbeidsgiver.min_side.services.tiltak

import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.stereotype.Service


@Service
class RefusjonStatusService(
    private val altinnService: AltinnService,
    private val refusjonStatusRepository: RefusjonStatusRepository,
) {

    fun statusoversikt(fnr: String): List<Statusoversikt> {
        val orgnr = altinnService
            .hentOrganisasjonerBasertPaRettigheter(fnr, TJENESTEKODE, TJENESTEVERSJON)
            .mapNotNull(Organisasjon::organizationNumber)

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