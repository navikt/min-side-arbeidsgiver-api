package no.nav.arbeidsgiver.min_side.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AltinnTilgangssøknad(
    var orgnr: String? = null,
    var serviceCode: String? = null,
    var serviceEdition: Int? = null,
    var status: String? = null,
    var createdDateTime: String? = null,
    var lastChangedDateTime: String? = null,
    var submitUrl: String? = null
)