package no.nav.tag.dittNavArbeidsgiver.services.unleash;

import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.UnleashContext;
import no.finn.unleash.Variant;
import no.finn.unleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;


@Configuration
public class DNAUnleashConfig {

    @Value("${unleash.url}")
    private String api;

    @Profile("dev")
    @Bean
    public Unleash testUnleash() {
        return new LocalLeash();
    }

    @Profile({"preprod", "prod"})
    @Bean
    public Unleash unleash() {
        UnleashConfig conf = UnleashConfig.builder()
                    .appName("ditt-nav-arbeidsgiver-api")
                    .instanceId(System.getProperty("environment.name", "local"))
                    .unleashAPI(api)
                    .build();

        return new DefaultUnleash(conf);
    }

    private static class LocalLeash implements Unleash {

        public LocalLeash() {
            // TODO Mocke respons fra Unleash-server?
        }

        @Override
        public boolean isEnabled(String s) {
            return true;
        }

        @Override
        public boolean isEnabled(String s, boolean b) {
            return true;
        }

        @Override
        public Variant getVariant(String s, UnleashContext unleashContext) {
            return null;
        }

        @Override
        public Variant getVariant(String s, UnleashContext unleashContext, Variant variant) {
            return null;
        }

        @Override
        public Variant getVariant(String s) {
            return null;
        }

        @Override
        public Variant getVariant(String s, Variant variant) {
            return null;
        }

        @Override
        public List<String> getFeatureToggleNames() {
            return null;
        }
    }
}