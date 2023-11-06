package no.nav.arbeidsgiver.min_side.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InnloggingsController {
    @GetMapping("/api/innlogget")
    fun erInnlogget() =
        ResponseEntity.notFound().build<Nothing>()
}