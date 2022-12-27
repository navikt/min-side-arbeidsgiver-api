package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.unleash.FeatureToggleService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
class FeatureToggleController(private val featureToggleService: FeatureToggleService) {
    @GetMapping("api/feature")
    fun feature(@RequestParam("feature") features: List<String>) =
        featureToggleService.hentFeatureToggles(features)
}