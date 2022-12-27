package no.nav.arbeidsgiver.min_side.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Organisasjon(
    @field:JsonProperty("Name") var name: String? = null,
    @field:JsonProperty("Type") var type: String? = null,
    @field:JsonProperty("ParentOrganizationNumber") var parentOrganizationNumber: String? = null,
    @field:JsonProperty("OrganizationNumber") var organizationNumber: String? = null,
    @field:JsonProperty("OrganizationForm") var organizationForm: String? = null,
    @field:JsonProperty("Status") var status: String? = null
)