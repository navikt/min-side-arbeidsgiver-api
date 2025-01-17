package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EregController(
    private val eregService: EregService
) {

    data class Request(
        val orgnr: String
    )

    @PostMapping("/api/ereg/underenhet")
    fun underenhet(@RequestBody request: Request): EregOrganisasjonDto? =
        EregOrganisasjonDto.fraEregOrganisasjon(eregService.hentUnderenhet(request.orgnr))

    @PostMapping("/api/ereg/overenhet")
    fun overenhet(@RequestBody request: Request): EregOrganisasjonDto? =
        EregOrganisasjonDto.fraEregOrganisasjon(eregService.hentOverenhet(request.orgnr))

    data class EregOrganisasjonDto(
        val organisasjonsnummer: String,
        val navn: String,
        val organisasjonsform: OrganisasjonsformDto? = null,
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
                    organisasjonsform = org.organisasjonDetaljer.enhetstyper?.firstOrNull() { it.gyldighetsperiode.erGyldig() }
                        .let { OrganisasjonsformDto.fraOrganisasjonsform(it?.enhetstype) },
                    naeringskoder = org.organisasjonDetaljer.naeringer?.filter { it.gyldighetsperiode.erGyldig() }
                        ?.mapNotNull { it.naeringskode },
                    postadresse = org.organisasjonDetaljer.postadresser?.firstOrNull { it.gyldighetsperiode.erGyldig() }
                        .let { EregAdresseDto.fraEregAdresse(it) },
                    forretningsadresse = org.organisasjonDetaljer.forretningsadresser?.first { it.gyldighetsperiode.erGyldig() }
                        .let { EregAdresseDto.fraEregAdresse(it) },
                    hjemmeside = org.organisasjonDetaljer.internettadresser?.firstOrNull { it.gyldighetsperiode.erGyldig() }?.adresse,
                    overordnetEnhet = org.orgnummerTilOverenhet(),
                    antallAnsatte = org.organisasjonDetaljer.ansatte?.firstOrNull { it.gyldighetsperiode.erGyldig() }?.antall,
                    beliggenhetsadresse = org.organisasjonDetaljer.forretningsadresser?.firstOrNull { it.gyldighetsperiode.erGyldig() }
                        .let { EregAdresseDto.fraEregAdresse(it) }
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
            fun fraEregAdresse(adresse: EregAdresse?): EregAdresseDto? {
                if (adresse == null) {
                    return null
                }
                return EregAdresseDto(
                    adresse = concatinateAddresse(adresse),
                    kommunenummer = adresse.kommunenummer,
                    land = landkodeTilLand[adresse.landkode],
                    landkode = adresse.landkode,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed
                )
            }

            private fun concatinateAddresse(adresse: EregAdresse): String {
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

data class OrganisasjonsformDto(
    val kode: String,
    val beskrivelse: String
) {
    companion object {
        fun fraOrganisasjonsform(organisasjonsForm: String?): OrganisasjonsformDto? {
            return OrganisasjonsformDto(
                kode = organisasjonsForm ?: "",
                beskrivelse = organisasjonsFormTilBeskrivelse[organisasjonsForm] ?: ""
            )
        }
    }
}

val organisasjonsFormTilBeskrivelse: Map<String, String> = mapOf(
    "AAFY" to "Underenhet til ikke-næringsdrivende",
    "ADOS" to "Administrativ enhet -offentlig sektor",
    "ANNA" to "Annen juridisk person",
    "ANS" to "Ansvarlig selskap med solidarisk ansvar",
    "AS" to "Aksjeselskap",
    "ASA" to "Allmennaksjeselskap",
    "BA" to "Selskap med begrenset ansvar",
    "BBL" to "Boligbyggelag",
    "BEDR" to "Underenhet til næringsdrivende og offentlig forvaltning",
    "BO" to "Andre bo",
    "BRL" to "Borettslag",
    "DA" to "Ansvarlig selskap med delt ansvar",
    "ENK" to "Enkeltpersonforetak",
    "EOFG" to "Europeisk økonomisk foretaksgruppe",
    "ESEK" to "Eierseksjonssameie",
    "FKF" to "Fylkeskommunalt foretak",
    "FLI" to "Forening/lag/innretning",
    "FYLK" to "Fylkeskommune",
    "GFS" to "Gjensidig forsikringsselskap",
    "IKJP" to "Andre ikke-juridiske personer",
    "IKS" to "Interkommunalt selskap",
    "KBO" to "Konkursbo",
    "KF" to "Kommunalt foretak",
    "KIRK" to "Den norske kirke",
    "KOMM" to "Kommune",
    "KS" to "Kommandittselskap",
    "KTRF" to "Kontorfellesskap",
    "NUF" to "Norskregistrert utenlandsk foretak",
    "OPMV" to "Særskilt oppdelt enhet, jf. mval. § 2-2",
    "ORGL" to "Organisasjonsledd",
    "PERS" to "Andre enkeltpersoner som registreres i tilknyttet register",
    "PK" to "Pensjonskasse",
    "PRE" to "Partrederi",
    "SA" to "Samvirkeforetak",
    "SAM" to "Tingsrettslig sameie",
    "SE" to "Europeisk selskap",
    "SF" to "Statsforetak",
    "SPA" to "Sparebank",
    "STAT" to "Staten",
    "STI" to "Stiftelse",
    "SÆR" to "Annet foretak iflg. særskilt lov",
    "TVAM" to "Tvangsregistrert for MVA",
    "UTLA" to "Utenlandsk enhet",
    "VPFO" to "Verdipapirfond",
)