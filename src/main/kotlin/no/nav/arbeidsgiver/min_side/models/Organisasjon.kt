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
) {
    companion object {
        @JvmStatic
        fun builder() = OrgBuilder()
    }

    // TODO: fjern denne når alt er konvertert til kotlin
    class OrgBuilder(
        var name: String? = null,
        var type: String? = null,
        var parentOrganizationNumber: String? = null,
        var organizationNumber: String? = null,
        var organizationForm: String? = null,
        var status: String? = null
    ) {
        fun name(name: String?): OrgBuilder {
            this.name = name
            return this
        }
        fun type(type: String?): OrgBuilder {
            this.type = type
            return this
        }
        fun parentOrganizationNumber(parentOrganizationNumber: String?): OrgBuilder {
            this.parentOrganizationNumber = parentOrganizationNumber
            return this
        }
        fun organizationNumber(organizationNumber: String?): OrgBuilder {
            this.organizationNumber = organizationNumber
            return this
        }
        fun organizationForm(organizationForm: String?): OrgBuilder {
            this.organizationForm = organizationForm
            return this
        }
        fun status(status: String?): OrgBuilder {
            this.status = status
            return this
        }
        fun build() = Organisasjon(
            name = name,
            type = type,
            parentOrganizationNumber = parentOrganizationNumber,
            organizationNumber = organizationNumber,
            organizationForm = organizationForm,
            status = status,
        )
    }
}