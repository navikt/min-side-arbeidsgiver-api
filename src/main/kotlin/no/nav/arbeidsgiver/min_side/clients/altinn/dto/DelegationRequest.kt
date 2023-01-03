package no.nav.arbeidsgiver.min_side.clients.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DelegationRequest(
    @field:JsonProperty("RequestStatus") var RequestStatus: String? = null,
    @field:JsonProperty("OfferedBy") var OfferedBy: String? = null,
    @field:JsonProperty("CoveredBy") var CoveredBy: String? = null,
    @field:JsonProperty("RedirectUrl") var RedirectUrl: String? = null,
    @field:JsonProperty("Created") var Created: String? = null,
    @field:JsonProperty("LastChanged") var LastChanged: String? = null,
    @field:JsonProperty("KeepSessionAlive") val KeepSessionAlive: Boolean = true,
    @field:JsonProperty("RequestResources") var RequestResources: List<RequestResource>? = null,
    @field:JsonProperty("_links") var links: Links? = null
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RequestResource(
        @field:JsonProperty("ServiceCode") var ServiceCode: String? = null,
        @field:JsonProperty("ServiceEditionCode") var ServiceEditionCode: Int? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Links(var sendRequest: Link? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Link(var href: String? = null)
}