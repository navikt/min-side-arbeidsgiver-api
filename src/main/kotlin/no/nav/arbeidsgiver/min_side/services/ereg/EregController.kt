package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EregController(
    private val eregService: EregService
) {
    @GetMapping("/api/ereg/underenhet")
    fun underenhet(orgnr: String): EregOrganisasjonDto? =
        EregOrganisasjonDto.fraEregOrganisasjon(eregService.hentUnderenhet(orgnr))

    @GetMapping("/api/ereg/overenhet")
    fun overenhet(orgnr: String): EregOrganisasjonDto? =
        EregOrganisasjonDto.fraEregOrganisasjon(eregService.hentOverenhet(orgnr))

    data class EregOrganisasjonDto(
        val organisasjonsnummer: String,
        val navn: String,
        val organisasjonsform: String? = null,
        val naeringskoder: List<String>? = null,
        val postadresse: EregAdresseDto? = null,
        val forretningsadresse: EregAdresseDto? = null,
        val hjemmeside: String? = null,
        val overordnetEnhet: String? = null,
        val antallAnsatte: Int? = null,
        val beliggenhetsadresse: EregAdresseDto? = null,
    ) {
        companion object {
            fun fraEregOrganisasjon(org: EregOrganisasjon?): EregOrganisasjonDto? {
                if (org == null) {
                    return null
                }
                return EregOrganisasjonDto(
                    organisasjonsnummer = org.organisasjonsnummer,
                    navn = org.navn.sammensattnavn,
                    organisasjonsform = org.organisasjonDetaljer.enhetstyper?.first { it.gyldighetsPeriode.erGyldig() }?.enhetstype,
                    naeringskoder = org.organisasjonDetaljer.naeringer?.filter { it.gyldighetsPeriode.erGyldig() }
                        ?.mapNotNull { it.naeringskode },
                    postadresse = EregAdresseDto.fraEregAdresse(org.organisasjonDetaljer.postadresser?.first { it.gyldighetsPeriode.erGyldig() }),
                    forretningsadresse = EregAdresseDto.fraEregAdresse(org.organisasjonDetaljer.forretningsadresser?.first { it.gyldighetsPeriode.erGyldig() }),
                    hjemmeside = org.organisasjonDetaljer.internettadresser?.firstOrNull { it.gyldighetsPeriode.erGyldig() }?.adresse,
                    overordnetEnhet = org.orgnummerTilOverenhet(),
                    antallAnsatte = org.organisasjonDetaljer.ansatte?.firstOrNull { it.gyldighetsPeriode.erGyldig() }?.antall,
                    beliggenhetsadresse = EregAdresseDto.fraEregAdresse(org.organisasjonDetaljer.forretningsadresser?.first { it.gyldighetsPeriode.erGyldig() })
                )
            }
        }
    }

    data class EregAdresseDto(
        val adresse: String? = null,
        val kommunenummer: String? = null,
        val land: String? = null,
        val landkode: String? = null,
        val postnummer: String? = null,
        val poststed: String? = null
    ) {
        companion object {
            fun fraEregAdresse(adresse: EregAdresse?): EregAdresseDto {
                if (adresse == null) {
                    null
                }
                return EregAdresseDto(
                    adresse = concatinateAddresse(adresse!!),
                    kommunenummer = adresse.kommunenummer,
                    land = landkodeTilLand[adresse.landkode],
                    landkode = adresse.landkode,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed
                )
            }

            private fun concatinateAddresse(adresse: EregAdresse) : String {
                var res = ""
                if (adresse.adresselinje1 != null) {
                    res += adresse.adresselinje1
                }
                if (adresse.adresselinje2 != null) {
                    res += adresse.adresselinje2
                }
                return res
            }
        }
    }
}
