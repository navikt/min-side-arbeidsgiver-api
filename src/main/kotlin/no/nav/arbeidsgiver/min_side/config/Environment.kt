package no.nav.arbeidsgiver.min_side.config

class Environment {
    class Sokos {
        companion object {
            val sokosKontoregisterBaseUrl = GittMiljø2.resolve(
                prod = { "https://sokos-kontoregister.prod-fss-pub.nais.io" },
                dev = { "https://sokos-kontoregister-q2.dev-fss-pub.nais.io" },
                other = { "http://localhost:8081" }
            )
        }
    }

    class Ereg {
        companion object {
            val eregServicesBaseUrl = GittMiljø2.resolve(
                prod = { "https://ereg-services.prod-fss-pub.nais.io" },
                dev = { "https://ereg-services.dev-fss-pub.nais.io" },
                other = { "http://localhost:8081" }
            )
        }
    }

    class Altinn {
        companion object {
            val altinnApiBaseUrl = GittMiljø2.resolve(
                prod = { "https://www.altinn.no" },
                dev = { "https://tt02.altinn.no" },
                other = { "http://localhost:8081" }
            )

            val altinnHeader = GittMiljø2.resolve(
                prod = { System.getenv("ALTINN_HEADER") },
                dev = { System.getenv("ALTINN_HEADER") },
                other = { "test" }
            )
        }
    }

    class TokenX {
        companion object {
            val privateJwk = GittMiljø2.resolve(
                prod = { System.getenv("TOKEN_X_PRIVATE_JWK") },
                dev = { System.getenv("TOKEN_X_PRIVATE_JWK") },
                other = { "fake" }
            )
            val clientId = GittMiljø2.resolve(
                prod = { System.getenv("TOKEN_X_CLIENT_ID") },
                dev = { System.getenv("TOKEN_X_CLIENT_ID") },
                other = { "fake" }
            )
            val issuer = GittMiljø2.resolve(
                prod = { System.getenv("TOKEN_X_ISSUER") },
                dev = { System.getenv("TOKEN_X_ISSUER") },
                other = { "http://fake" }
            )
            val tokenEndpoint = GittMiljø2.resolve(
                prod = { System.getenv("TOKEN_X_TOKEN_ENDPOINT") },
                dev = { System.getenv("TOKEN_X_TOKEN_ENDPOINT") },
                other = { "http://fake/token" }
            )
            val tokenIntrospecionEndpint = GittMiljø2.resolve(
                prod = { System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") },
                dev = { System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") },
                other = { "http://localhost:8081/tokenIntrospection" }
            )
        }
    }
}
