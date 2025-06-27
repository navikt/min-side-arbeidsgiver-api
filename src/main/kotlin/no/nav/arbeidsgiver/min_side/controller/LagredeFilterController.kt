package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import org.springframework.web.bind.annotation.*

@RestController
class LagredeFilterController(
    val lagredeFilterService: LagredeFilterService,
    val authenticatedUserHolder: AuthenticatedUserHolder,
) {
    @GetMapping(
        path = [
            "/api/lagredeFilter",
            "/api/lagredeFilter/",
        ]
    )
    fun get() = lagredeFilterService.getAll(authenticatedUserHolder.fnr)

    @DeleteMapping(
        path = [
            "/api/lagredeFilter/{filterId}",
            "/api/lagredeFilter/{filterId}/",
        ]
    )
    fun delete(@PathVariable filterId: String) = lagredeFilterService.delete(authenticatedUserHolder.fnr, filterId)

    @PutMapping(
        path = [
            "/api/lagredeFilter",
            "/api/lagredeFilter/",
        ]
    )
    fun put(
        @RequestBody filter: LagredeFilterService.LagretFilter
    ) {
        lagredeFilterService.put(authenticatedUserHolder.fnr, filter)
    }
}