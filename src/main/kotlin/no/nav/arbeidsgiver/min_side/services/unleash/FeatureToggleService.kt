package no.nav.arbeidsgiver.min_side.services.unleash

import no.finn.unleash.Unleash
import no.finn.unleash.UnleashContext
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    private val unleash: Unleash,
    private val authenticatedUserHolder: AuthenticatedUserHolder
) {
    fun hentFeatureToggles(features: List<String>) =
        features.associateWith { isEnabled(it) }

    fun isEnabled(feature: String) =
        unleash.isEnabled(feature, contextMedInnloggetBruker())

    private fun contextMedInnloggetBruker() =
        UnleashContext.builder().userId(authenticatedUserHolder.token).build()
}