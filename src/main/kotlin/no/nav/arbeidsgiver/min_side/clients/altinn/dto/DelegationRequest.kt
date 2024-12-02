package no.nav.arbeidsgiver.min_side.clients.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DelegationRequest(
    var RequestStatus: String? = null,
    var OfferedBy: String? = null,
    var CoveredBy: String? = null,
    var RedirectUrl: String? = null,
    var Created: String? = null,
    var LastChanged: String? = null,
    val KeepSessionAlive: Boolean = true,
    var RequestResources: List<RequestResource>? = null,
    @field:JsonProperty("_links") var links: Links? = null
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RequestResource(
        var ServiceCode: String? = null,
        var ServiceEditionCode: Int? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Links(var sendRequest: Link? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Link(var href: String? = null)
}