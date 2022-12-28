package no.nav.arbeidsgiver.min_side.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = AuthenticatedUserHolder.TOKENX, claimMap = [AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL])
@RestController
class InnloggingsController {
    @GetMapping("/api/innlogget")
    fun erInnlogget() =
        ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body("ok")
}