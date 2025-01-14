package no.nav.arbeidsgiver.min_side.models

import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon

data class Organisasjon(
    var name: String,
    var parentOrganizationNumber: String? = null,
    var organizationNumber: String,
    var organizationForm: String,
    ) {
    companion object {
        fun fromEregOrganisasjon(eregOrganisasjon: EregOrganisasjon?): Organisasjon? {
            if (eregOrganisasjon == null) {
                return null
            }
            return Organisasjon(
                name = eregOrganisasjon.navn,
                parentOrganizationNumber = eregOrganisasjon.overordnetEnhet,
                organizationNumber = eregOrganisasjon.organisasjonsnummer,
                organizationForm = eregOrganisasjon.organisasjonsform
            )
        }
    }
}
