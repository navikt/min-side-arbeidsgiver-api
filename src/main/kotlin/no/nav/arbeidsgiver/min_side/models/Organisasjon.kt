package no.nav.arbeidsgiver.min_side.models

import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig

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
                name = eregOrganisasjon.navn.sammensattnavn,
                parentOrganizationNumber = eregOrganisasjon.orgnummerTilOverenhet(),
                organizationNumber = eregOrganisasjon.organisasjonsnummer,
                organizationForm = eregOrganisasjon.organisasjonDetaljer.enhetstyper?.first { it.gyldighetsperiode.erGyldig() }?.enhetstype ?: ""
            )
        }
    }
}
