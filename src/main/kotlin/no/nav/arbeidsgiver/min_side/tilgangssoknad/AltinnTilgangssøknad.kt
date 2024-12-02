package no.nav.arbeidsgiver.min_side.tilgangssoknad

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AltinnTilgangssøknad(
    val orgnr: String? = null,
    val serviceCode: String? = null,
    val serviceEdition: Int? = null,
    val status: String? = null,
    val createdDateTime: String? = null,
    val lastChangedDateTime: String? = null,
    val submitUrl: String? = null
)