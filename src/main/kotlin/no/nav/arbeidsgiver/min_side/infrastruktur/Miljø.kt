package no.nav.arbeidsgiver.min_side.infrastruktur

class Miljø {
    class Sokos {
        companion object {
            val kontoregisterBaseUrl = resolve(
                prod = { "https://sokos-kontoregister.prod-fss-pub.nais.io" },
                dev = { "https://sokos-kontoregister-q2.dev-fss-pub.nais.io" },
                other = { "http://localhost:8081" }
            )
        }
    }

    class Ereg {
        companion object {
            val baseUrl = resolve(
                prod = { "https://ereg-services.prod-fss-pub.nais.io" },
                dev = { "https://ereg-services.dev-fss-pub.nais.io" },
                other = { "http://localhost:8081" }
            )
        }
    }

    class Altinn {
        companion object {
            val baseUrl = resolve(
                prod = { "https://www.altinn.no" },
                dev = { "https://tt02.altinn.no" },
                other = { "http://localhost:8081" }
            )

            val altinnHeader: String = resolve(
                prod = { System.getenv("ALTINN_HEADER") },
                dev = { System.getenv("ALTINN_HEADER") },
                other = { "test" }
            )
        }
    }

    companion object {
        val clusterName: String = System.getenv("NAIS_CLUSTER_NAME") ?: "local"

        fun <T> resolve(
            other: () -> T,
            prod: () -> T = other,
            dev: () -> T = other,
        ): T =
            when (clusterName) {
                "prod-gcp" -> prod()
                "dev-gcp" -> dev()
                else -> other()
            }
    }
}