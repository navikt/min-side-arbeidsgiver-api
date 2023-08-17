package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


private const val TJENESTEKODE = "4936"
private const val TJENESTEVERSJON = "1"

@RestController
class RefusjonStatusController(
    private val altinnService: AltinnService,
    private val refusjonStatusRepository: RefusjonStatusRepository,
    private val authenticatedUserHolder: AuthenticatedUserHolder
) {

    @GetMapping("/api/refusjon_status")
    fun statusoversikt(): List<Statusoversikt> {
        /* Man kan muligens filtrere organisasjoner ytligere med ("BEDR", annet?). */
        val orgnr = altinnService
            .hentOrganisasjonerBasertPaRettigheter(authenticatedUserHolder.fnr, TJENESTEKODE, TJENESTEVERSJON)
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
}