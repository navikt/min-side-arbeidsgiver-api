package no.nav.arbeidsgiver.min_side.services.unleash;


import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.util.UnleashConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FeatureToggleConfig {

    private static final String APP_NAME = "ditt-nav-arbeidsgiver-api";
    private final static String UNLEASH_API_URL = "https://unleash.nais.io/api/";
    @Bean
    @Profile({"dev-gcp","prod-gcp", "dev", "prod"})
    public Unleash initializeUnleash(
                                     ByClusterStrategy byClusterStrategy)

    {
        UnleashConfig config = UnleashConfig.builder()
                .appName(APP_NAME)
                .instanceId(APP_NAME + "-" + System.getProperty("environment.name", "local"))
                .unleashAPI(UNLEASH_API_URL)
                .build();

        return new DefaultUnleash(
                config,
                byClusterStrategy
        );
    }

    @Profile({"local","labs"})
    @Bean
    public Unleash unleashMock() {
        FakeUnleash fakeUnleash = new FakeUnleash();
        fakeUnleash.enableAll();
        return fakeUnleash;
    }
}

