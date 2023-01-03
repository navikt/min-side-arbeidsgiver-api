package no.nav.arbeidsgiver.min_side.clients.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Søknadsstatus(
    @field:JsonProperty("_embedded")
    var embedded: Embedded? = null,
    var continuationtoken: String? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Embedded(
        var delegationRequests: List<DelegationRequest>? = null
    )
}