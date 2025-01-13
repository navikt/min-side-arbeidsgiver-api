package no.nav.arbeidsgiver.min_side.services.ereg

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EregController(
    private val eregService: EregService
) {
    @GetMapping("/api/ereg/underenhet/{orgnr}")
    fun underenhet(orgnr: String): EregOrganisasjon? = eregService.hentOverenhet(orgnr)

    @GetMapping("/api/ereg/overenhet/{orgnr}")
    fun overenhet(orgnr: String): EregOrganisasjon? = eregService.hentUnderenhet(orgnr)
}