package no.nav.arbeidsgiver.min_side.config

class Environment {
    companion object {
        val sokosKontoregisterBaseUrl = GittMiljø2.resolve(
            prod = { "https://sokos-kontoregister.prod-fss-pub.nais.io" },
            dev = { "https://sokos-kontoregister-q2.dev-fss-pub.nais.io" },
            other = { "https://localhost" }
        )

        val eregServicesBaseUrl = GittMiljø2.resolve(
            prod = { "https://ereg-services.prod-fss-pub.nais.io" },
            dev = { "https://ereg-services.dev-fss-pub.nais.io" },
            other = { "https://localhost" }
        )

        val altinnApiBaseUrl = GittMiljø2.resolve(
            prod = { "https://www.altinn.no" },
            dev = { "https://tt02.altinn.no" },
            other = { "http://altinn.example.org" }
        )

        val altinnHeader = GittMiljø2.resolve(
            prod = { System.getenv("ALTINN_HEADER") },
            dev = { System.getenv("ALTINN_HEADER") },
            other = { "test" }
        )
    }
}