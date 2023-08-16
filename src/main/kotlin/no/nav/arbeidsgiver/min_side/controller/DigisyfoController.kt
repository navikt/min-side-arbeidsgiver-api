package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.Companion.ACR_CLAIM_NEW
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.Companion.ACR_CLAIM_OLD
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.Companion.TOKENX
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(
    issuer = TOKENX,
    claimMap = [ACR_CLAIM_OLD, ACR_CLAIM_NEW],
    combineWithOr = true,
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