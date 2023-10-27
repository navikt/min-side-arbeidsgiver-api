package no.nav.arbeidsgiver.min_side.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DigisyfoController {

    @GetMapping("/api/narmesteleder/virksomheter-v3")
    fun hentVirksomheter() =
        ResponseEntity.notFound().build<Nothing>()
}