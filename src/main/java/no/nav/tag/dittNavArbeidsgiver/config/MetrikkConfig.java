package no.nav.tag.dittNavArbeidsgiver.config;

import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "dev", "prod" })
public class MetrikkConfig {
    public MetrikkConfig() {
        String miljø = System.getenv("NAIS_CLUSTER_NAME");
        MetricsClient.enableMetrics(MetricsConfig.resolveNaisConfig().withEnvironment(miljø));
    }
}