package no.nav.arbeidsgiver.min_side.tilgangssoknad

data class AltinnTilgangssøknadsskjema(
    val orgnr: String,
    val redirectUrl: String,
    val serviceCode: String,
    val serviceEdition: Int,
)