package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EregController(
    private val eregService: EregService
) {
    @GetMapping("/api/ereg/underenhet/{orgnr}")
    fun underenhet(orgnr: String): EregOrganisasjonDto? =
        EregOrganisasjonDto.fraEregOrganisasjon(eregService.hentOverenhet(orgnr))

    @GetMapping("/api/ereg/overenhet/{orgnr}")
    fun overenhet(orgnr: String): EregOrganisasjonDto? =
        EregOrganisasjonDto.fraEregOrganisasjon(eregService.hentUnderenhet(orgnr))

    data class EregOrganisasjonDto(
        val organisasjonsnummer: String,
        val navn: String,
        val organisasjonsform: Kode? = null,
        val naeringer: List<Kode>? = null,
        val postadresse: EregAdresseDto? = null,
        val forretningsadresse: EregAdresseDto? = null,
        val hjemmeside: String? = null,
        val overordnetEnhet: String? = null,
        val antallAnsatte: Int? = null,
        val beliggenhetsadresse: EregAdresseDto? = null,
    ) {
        companion object {
            fun fraEregOrganisasjon(org: EregOrganisasjon?): EregOrganisasjonDto {
                if (org == null) {
                    null
                }
                return EregOrganisasjonDto(
                    organisasjonsnummer = org!!.organisasjonsnummer,
                    navn = org.navn.sammensattnavn,
                    organisasjonsform = org.organisasjonsDetaljer.enhetstyper?.first { it.gyldighetsPeriode.erGyldig() }.let { Kode(it?.enhetstype, "") }, //TODO: mappe denne?
                    naeringer = org.organisasjonsDetaljer.naeringer?.filter { it.gyldighetsPeriode.erGyldig() }?.map { Kode(it.naeringskode, "")}, //TODO: mappe denne?
                    postadresse = EregAdresseDto.fraEregAdresse(org.organisasjonsDetaljer.postadresser?.first { it.gyldighetsPeriode.erGyldig() }),
                    forretningsadresse = EregAdresseDto.fraEregAdresse(org.organisasjonsDetaljer.forretningsadresser?.first { it.gyldighetsPeriode.erGyldig() }),
                    hjemmeside = org.organisasjonsDetaljer.internettadresser?.firstOrNull { it.gyldighetsPeriode.erGyldig() }?.adresse,
                    overordnetEnhet = org.orgnummerTilOverenhet(),
                    antallAnsatte = org.organisasjonsDetaljer.ansatte?.firstOrNull { it.gyldighetsPeriode.erGyldig() }?.antall,
                    beliggenhetsadresse = EregAdresseDto.fraEregAdresse(org.organisasjonsDetaljer.forretningsadresser?.first { it.gyldighetsPeriode.erGyldig() })
                )
            }
        }
    }

    data class EregAdresseDto(
        val adresse: String? = null,
        val kommune: String? = null,
        val kommunenummer: String? = null,
        val land: String? = null,
        val landkode: String? = null,
        val postnummer: String? = null,
        val poststed: String? = null
    ) {
        companion object {
            fun fraEregAdresse(adresse: EregAdresse?) : EregAdresseDto{
                if (adresse == null) {
                    null
                }
                return EregAdresseDto(
                    adresse = adresse!!.adresse,
                    kommune = adresse.kommune,
                    kommunenummer = adresse.kommunenummer,
                    land = adresse.landkode, // TODO: mappe denne
                    landkode = adresse.landkode,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed
                )
            }
        }
    }
}
