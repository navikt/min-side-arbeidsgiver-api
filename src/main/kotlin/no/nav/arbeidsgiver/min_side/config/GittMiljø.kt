package no.nav.arbeidsgiver.min_side.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GittMiljø(
    @Value("\${spring.profiles.active}") private val miljø: String,
) {

    fun <T> resolve(
        other: () -> T,
        prod: () -> T = other,
        dev: () -> T = other,
    ): T =
        when (miljø) {
            "prod-gcp" -> prod()
            "dev-gcp" -> dev()
            else -> other()
        }
}

class GittMiljø2 {
    companion object {
        fun <T> resolve(
            other: () -> T,
            prod: () -> T = other,
            dev: () -> T = other,
        ): T =
            when (System.getenv("NAIS_CLUSTER_NAME") ?: "local") {
                "prod-gcp" -> prod()
                "dev-gcp" -> dev()
                else -> other()
            }
    }
}