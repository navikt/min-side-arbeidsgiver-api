package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrganisasjonController(
    val altinnService: AltinnService,
    val authenticatedUserHolder: AuthenticatedUserHolder,
) {
    @GetMapping("/api/organisasjoner")
    fun hentOrganisasjoner() =
        altinnService.hentOrganisasjoner(authenticatedUserHolder.fnr)

    @GetMapping(path = [
        "/api/rettigheter-til-skjema",
        "/api/rettigheter-til-skjema/",
    ])
    fun hentRettigheter(
        @RequestParam serviceKode: String,
        @RequestParam serviceEdition: String,
    ) = altinnService.hentOrganisasjonerBasertPaRettigheter(
        authenticatedUserHolder.fnr,
        serviceKode,
        serviceEdition
    )
}