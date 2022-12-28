package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(
    issuer = AuthenticatedUserHolder.TOKENX,
    claimMap = [AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL]
)
@RestController
class DigisyfoController(
    private val digisyfoService: DigisyfoService,
    private val authenticatedUserHolder: AuthenticatedUserHolder
) {

    data class VirksomhetOgAntallSykmeldte(
        val organisasjon: Organisasjon,
        val antallSykmeldte: Int,
    )

    @GetMapping("/api/narmesteleder/virksomheter-v3")
    fun hentVirksomheter(): Collection<VirksomhetOgAntallSykmeldte> =
        digisyfoService.hentVirksomheterOgSykmeldte(authenticatedUserHolder.fnr)
}