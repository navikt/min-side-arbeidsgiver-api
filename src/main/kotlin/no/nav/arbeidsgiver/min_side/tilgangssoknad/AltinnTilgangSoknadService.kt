package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.http.HttpStatusCode
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.web.client.HttpClientErrorException

class AltinnTilgangSoknadService(
    private val altinnTilgangssøknadClient: AltinnTilgangssøknadClient,
    private val altinnService: AltinnService,
) {
    private val log = logger()

    suspend fun mineSøknaderOmTilgang(fnr: String): List<AltinnTilgangssøknad> {
        return altinnTilgangssøknadClient.hentSøknader(fnr)
    }

    suspend fun sendSøknadOmTilgang(søknadsskjema: AltinnTilgangssøknadsskjema, token: String, fnr: String): ResponseEntity {
        val brukerErIOrg = altinnService.harOrganisasjon(søknadsskjema.orgnr, token)

        if (!brukerErIOrg) {
            log.error("Bruker forsøker å be om tilgang til org de ikke er med i.")
            return ResponseEntity(HttpStatusCode.BadRequest)
        }

        if (!tjenester.contains(søknadsskjema.serviceCode to søknadsskjema.serviceEdition)) {
            log.error(
                "Bruker forsøker å be om tilgang til tjeneste ({}, {})) vi ikke støtter.",
                søknadsskjema.serviceCode,
                søknadsskjema.serviceEdition
            )
            return ResponseEntity(HttpStatusCode.BadRequest)
        }
        val body = try {
            altinnTilgangssøknadClient.sendSøknad(fnr, søknadsskjema)
        } catch (e: HttpClientErrorException) {
            if (e.responseBodyAsString.contains("40318")) {
                // Bruker forsøker å sende en søknad som allerede er sendt.
                return ResponseEntity(HttpStatusCode.BadRequest)
            } else {
                throw e
            }
        }
        return ResponseEntity(HttpStatusCode.OK, body)
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

    data class ResponseEntity(
        val status: HttpStatusCode,
        val body: AltinnTilgangssøknad? = null,
    )
}