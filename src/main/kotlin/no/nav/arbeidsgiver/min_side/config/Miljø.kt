package no.nav.arbeidsgiver.min_side.config

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

            val altinnHeader = resolve(
                prod = { System.getenv("ALTINN_HEADER") },
                dev = { System.getenv("ALTINN_HEADER") },
                other = { "test" }
            )
        }
    }

    class AltinnTilgangerProxy {
        companion object {
            val url = resolve(
                prod = { "http://arbeidsgiver-altinn-tilganger/altinn-tilganger" },
                dev = { "http://arbeidsgiver-altinn-tilganger/altinn-tilganger" },
                other = { "http://localhost:8081/altinn-tilganger" }
            )
        }
    }

    class TokenX {
        companion object {
            val privateJwk = resolve(
                prod = { System.getenv("TOKEN_X_PRIVATE_JWK") },
                dev = { System.getenv("TOKEN_X_PRIVATE_JWK") },
                other = { "fake" }
            )
            val clientId = resolve(
                prod = { System.getenv("TOKEN_X_CLIENT_ID") },
                dev = { System.getenv("TOKEN_X_CLIENT_ID") },
                other = { "fake" }
            )
            val issuer = resolve(
                prod = { System.getenv("TOKEN_X_ISSUER") },
                dev = { System.getenv("TOKEN_X_ISSUER") },
                other = { "http://fake" }
            )
            val tokenEndpoint = resolve(
                prod = { System.getenv("TOKEN_X_TOKEN_ENDPOINT") },
                dev = { System.getenv("TOKEN_X_TOKEN_ENDPOINT") },
                other = { "http://localhost:8081/token" }
            )
            val tokenIntrospecionEndpint = resolve(
                prod = { System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") },
                dev = { System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") },
                other = { "http://localhost:8081/tokenIntrospection" }
            )
        }
    }

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
