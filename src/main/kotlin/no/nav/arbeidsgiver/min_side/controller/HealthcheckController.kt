package no.nav.arbeidsgiver.min_side.controller

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
class HealthcheckController {
    @GetMapping("/internal/isAlive")
    fun isAlive() = "ok"

    @GetMapping("/internal/isReady")
    fun isReady() = "ok"
}