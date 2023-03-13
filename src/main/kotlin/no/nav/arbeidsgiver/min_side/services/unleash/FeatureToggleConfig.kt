package no.nav.arbeidsgiver.min_side.services.unleash

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.util.UnleashConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FeatureToggleConfig {
    companion object {
        private const val APP_NAME = "ditt-nav-arbeidsgiver-api"
        private const val UNLEASH_API_URL = "https://unleash.nais.io/api/"
    }

    @Bean
    @Profile("dev-gcp", "prod-gcp")
    fun initializeUnleash(byClusterStrategy: ByClusterStrategy) =
        DefaultUnleash(
            UnleashConfig.builder()
                .appName(APP_NAME)
                .instanceId(APP_NAME + "-" + System.getProperty("environment.name", "local"))
                .unleashAPI(UNLEASH_API_URL)
                .build(),
            byClusterStrategy
        )

    @Bean
    @Profile("local", "demo")
    fun unleashMock() = FakeUnleash().apply { enableAll() }
}