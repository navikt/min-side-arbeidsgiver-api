package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(
    issuer = AuthenticatedUserHolder.TOKENX,
    claimMap = [AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL]
)
@RestController
class OrganisasjonController(
    val altinnService: AltinnService,
    val authenticatedUserHolder: AuthenticatedUserHolder,
) {
    @GetMapping("/api/organisasjoner")
    fun hentOrganisasjoner() =
        altinnService.hentOrganisasjoner(authenticatedUserHolder.fnr)

    @GetMapping("/api/rettigheter-til-skjema")
    fun hentRettigheter(
        @RequestParam serviceKode: String,
        @RequestParam serviceEdition: String,
    ) = altinnService.hentOrganisasjonerBasertPaRettigheter(
        authenticatedUserHolder.fnr,
        serviceKode,
        serviceEdition
    )
}