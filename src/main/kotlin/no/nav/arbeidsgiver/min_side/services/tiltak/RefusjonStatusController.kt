package no.nav.arbeidsgiver.min_side.services.tiltak

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class RefusjonStatusController(
    private val refusjonStatusService: RefusjonStatusService,
    private val authenticatedUserHolder: AuthenticatedUserHolder
) {

    @GetMapping("/api/refusjon_status")
    fun statusoversikt() = refusjonStatusService.statusoversikt(authenticatedUserHolder.fnr)
}