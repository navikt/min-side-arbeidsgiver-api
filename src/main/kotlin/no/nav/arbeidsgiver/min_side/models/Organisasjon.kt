package no.nav.arbeidsgiver.min_side.models

import com.fasterxml.jackson.annotation.JsonProperty

data class Organisasjon(
    @field:JsonProperty("Name") var name: String,
    @field:JsonProperty("ParentOrganizationNumber") var parentOrganizationNumber: String? = null,
    @field:JsonProperty("OrganizationNumber") var organizationNumber: String,
    @field:JsonProperty("OrganizationForm") var organizationForm: String,
)