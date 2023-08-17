package no.nav.arbeidsgiver.min_side.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthcheckController {
    @GetMapping("/internal/isAlive")
    fun isAlive() = "ok"

    @GetMapping("/internal/isReady")
    fun isReady() = "ok"
}