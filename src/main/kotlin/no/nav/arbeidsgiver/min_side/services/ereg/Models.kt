package no.nav.arbeidsgiver.min_side.services.ereg

data class Adresse (
    val type: String,
    val adresse: String?,
    val kommune: String?,
    val kommunenummer: String?,
    val land: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class EregOrganisasjon (
    val organisasjonsnummer: String,
    val navn: String,
    val organisasjonsform: String,
    val naeringskoder: List<Kode>?,
    val postadresse: Adresse?,
    val forretningsadresse: Adresse?,
    val hjemmeside: String?,
    val overordnetEnhet: String?,
    val antallAnsatte: Int?,
    val harRegistrertAntallAnsatte: Boolean = antallAnsatte != null,
    val beliggenhetsadresse: Adresse? = postadresse, //TODO: usikker her
)

data class Kode (
    val kode: String?,
    val beskrivelse: String?
)