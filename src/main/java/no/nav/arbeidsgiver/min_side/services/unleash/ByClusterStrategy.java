package no.nav.arbeidsgiver.min_side.services.unleash;

import no.finn.unleash.strategy.Strategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class ByClusterStrategy implements Strategy {
    private final String cluster;

    public ByClusterStrategy(
            @Value("${nais.cluster.name}") String cluster
    ) {
        this.cluster = cluster;
    }

    @Override
    public String getName() {
        return "byCluster";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        if (parameters == null) {
            return false;
        }

        String clusterParameter = parameters.get("cluster");
        if (clusterParameter == null) {
            return false;
        }

        String[] alleClustere = clusterParameter.split(",");
        return Arrays.asList(alleClustere).contains(cluster);
    }
}
