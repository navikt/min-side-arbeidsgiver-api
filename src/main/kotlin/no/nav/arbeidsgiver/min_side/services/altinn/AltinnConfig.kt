package no.nav.arbeidsgiver.min_side.services.altinn

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "altinn")
data class AltinnConfig(
    var altinnHeader: String = "",
    var altinnurl: String = "",
    var APIGwHeader: String = "",
    var APIGwUrl: String = "",
)