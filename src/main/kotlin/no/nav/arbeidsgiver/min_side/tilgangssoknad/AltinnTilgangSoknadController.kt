package no.nav.arbeidsgiver.min_side.tilgangssoknad

import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api/altinn-tilgangssoknad")
class AltinnTilgangSoknadController(
    private val altinnTilgangssøknadClient: AltinnTilgangssøknadClient,
    private val altinnService: AltinnService,
    private val authenticatedUserHolder: AuthenticatedUserHolder
) {
    private val log = logger()

    @GetMapping
    fun mineSøknaderOmTilgang(): List<AltinnTilgangssøknad> {
        val fødselsnummer = authenticatedUserHolder.fnr
        return altinnTilgangssøknadClient.hentSøknader(fødselsnummer)
    }

    @PostMapping
    fun sendSøknadOmTilgang(@RequestBody søknadsskjema: AltinnTilgangssøknadsskjema): ResponseEntity<AltinnTilgangssøknad> {
        val brukerErIOrg = altinnService.harOrganisasjon(søknadsskjema.orgnr)

        if (!brukerErIOrg) {
            log.error("Bruker forsøker å be om tilgang til org de ikke er med i.")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }

        if (!tjenester.contains(søknadsskjema.serviceCode to søknadsskjema.serviceEdition)) {
            log.error(
                "Bruker forsøker å be om tilgang til tjeneste ({}, {})) vi ikke støtter.",
                søknadsskjema.serviceCode,
                søknadsskjema.serviceEdition
            )
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val body = try {
            altinnTilgangssøknadClient.sendSøknad(authenticatedUserHolder.fnr, søknadsskjema)
        } catch (e: HttpClientErrorException) {
            if (e.responseBodyAsString.contains("40318")) {
                // Bruker forsøker å sende en søknad som allerede er sendt.
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
            } else {
                throw e
            }
        }
        return ResponseEntity.ok(body)
    }

    companion object {
        val tjenester = setOf(
            "3403" to 2,
            "4936" to 1,
            "5078" to 1,
            "5159" to 1,
            "5212" to 1,
            "5216" to 1,
            "5278" to 1,
            "5332" to 1,
            "5332" to 2,
            "5384" to 1,
            "5441" to 1,
            "5516" to 1,
            "5516" to 2,
            "5516" to 3,
            "5516" to 4,
            "5516" to 5,
            "5516" to 6,
            "5516" to 7,
            "5902" to 1,
            "5934" to 1,
        )
    }
}