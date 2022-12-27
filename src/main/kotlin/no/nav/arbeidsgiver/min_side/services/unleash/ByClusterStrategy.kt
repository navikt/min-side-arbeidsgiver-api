package no.nav.arbeidsgiver.min_side.services.unleash

import no.finn.unleash.strategy.Strategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ByClusterStrategy(
    @param:Value("\${nais.cluster.name}") private val cluster: String
) : Strategy {
    override fun getName() = "byCluster"

    override fun isEnabled(parameters: Map<String, String>) =
        when (val clusterParameter = parameters["cluster"]) {
            null -> false
            else -> clusterParameter.split(",").contains(cluster)
        }
}