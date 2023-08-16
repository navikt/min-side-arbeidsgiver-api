package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.Companion.ACR_CLAIM_NEW
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.Companion.ACR_CLAIM_OLD
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.Companion.TOKENX
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(
    issuer = TOKENX,
    claimMap = [ACR_CLAIM_OLD, ACR_CLAIM_NEW],
    combineWithOr = true,
)
@RestController
class InnloggingsController {
    @GetMapping("/api/innlogget")
    fun erInnlogget() =
        ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body("ok")
}