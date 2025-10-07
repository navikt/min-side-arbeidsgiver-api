package no.nav.arbeidsgiver.min_side.config

class Environment {
    class Sokos {
        companion object {
            val sokosKontoregisterBaseUrl = GittMiljø.resolve(
                prod = { "https://sokos-kontoregister.prod-fss-pub.nais.io" },
                dev = { "https://sokos-kontoregister-q2.dev-fss-pub.nais.io" },
                other = { "http://localhost:8081" }
            )
        }
    }

    class Ereg {
        companion object {
            val eregServicesBaseUrl = GittMiljø.resolve(
                prod = { "https://ereg-services.prod-fss-pub.nais.io" },
                dev = { "https://ereg-services.dev-fss-pub.nais.io" },
                other = { "http://localhost:8081" }
            )
        }
    }

    class Altinn {
        companion object {
            val altinnApiBaseUrl = GittMiljø.resolve(
                prod = { "https://www.altinn.no" },
                dev = { "https://tt02.altinn.no" },
                other = { "http://localhost:8081" }
            )

            val altinnHeader = GittMiljø.resolve(
                prod = { System.getenv("ALTINN_HEADER") },
                dev = { System.getenv("ALTINN_HEADER") },
                other = { "test" }
            )
        }
    }

    class AltinnTilgangerProxy {
        companion object {
            val altinnTilgangerUrl = GittMiljø.resolve(
                prod = { "http://arbeidsgiver-altinn-tilganger/altinn-tilganger" },
                dev = { "http://arbeidsgiver-altinn-tilganger/altinn-tilganger" },
                other = { "http://localhost:8081/altinn-tilganger" }
            )
        }
    }

    class TokenX {
        companion object {
            val privateJwk = GittMiljø.resolve(
                prod = { System.getenv("TOKEN_X_PRIVATE_JWK") },
                dev = { System.getenv("TOKEN_X_PRIVATE_JWK") },
                other = { "fake" }
            )
            val clientId = GittMiljø.resolve(
                prod = { System.getenv("TOKEN_X_CLIENT_ID") },
                dev = { System.getenv("TOKEN_X_CLIENT_ID") },
                other = { "fake" }
            )
            val issuer = GittMiljø.resolve(
                prod = { System.getenv("TOKEN_X_ISSUER") },
                dev = { System.getenv("TOKEN_X_ISSUER") },
                other = { "http://fake" }
            )
            val tokenEndpoint = GittMiljø.resolve(
                prod = { System.getenv("TOKEN_X_TOKEN_ENDPOINT") },
                dev = { System.getenv("TOKEN_X_TOKEN_ENDPOINT") },
                other = { "http://localhost:8081/token" }
            )
            val tokenIntrospecionEndpint = GittMiljø.resolve(
                prod = { System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") },
                dev = { System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") },
                other = { "http://localhost:8081/tokenIntrospection" }
            )
        }
    }
}
