package no.nav.arbeidsgiver.min_side.models

data class Organisasjon(
    var name: String,
    var parentOrganizationNumber: String? = null,
    var organizationNumber: String,
    var organizationForm: String,
)