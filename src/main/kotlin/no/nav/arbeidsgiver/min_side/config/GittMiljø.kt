package no.nav.arbeidsgiver.min_side.config

class GittMiljø {
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