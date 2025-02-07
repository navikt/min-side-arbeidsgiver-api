package no.nav.arbeidsgiver.min_side.services.ereg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.arbeidsgiver.min_side.config.callIdIntercetor
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import java.net.SocketException
import java.time.LocalDate
import javax.net.ssl.SSLHandshakeException


@Component
class EregService(
    @Value("\${ereg-services.baseUrl}") eregBaseUrl: String?,
    restTemplateBuilder: RestTemplateBuilder
) {
    internal val restTemplate = restTemplateBuilder
        .rootUri(eregBaseUrl)
        .additionalInterceptors(
            callIdIntercetor("Nav-Call-Id"),
            retryInterceptor(
                3,
                250L,
                SocketException::class.java,
                SSLHandshakeException::class.java,
            )
        )
        .build()

    @Cacheable(EREG_CACHE_NAME)
    fun hentUnderenhet(virksomhetsnummer: String): EregOrganisasjon? {
        return try {
            val request = restTemplate.getForEntity(
                "/v2/organisasjon/{virksomhetsnummer}?inkluderHierarki=true",
                EregOrganisasjon::class.java,
                mapOf("virksomhetsnummer" to virksomhetsnummer)
            )
            request.body
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    @Cacheable(EREG_CACHE_NAME)
    fun hentOverenhet(orgnummer: String): EregOrganisasjon? {
        return try {
            restTemplate.getForEntity(
                "/v2/organisasjon/{orgnummer}?inkluderHierarki=true",
                EregOrganisasjon::class.java,
                mapOf(
                    "orgnummer" to orgnummer,
                )
            ).body
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }
}

const val EREG_CACHE_NAME = "ereg_cache"

@Configuration
class EregCacheConfig {

    @Bean
    fun eregCache() = CaffeineCache(
        EREG_CACHE_NAME,
        Caffeine.newBuilder()
            .maximumSize(600000)
            .recordStats()
            .build()
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregAdresse(
    val type: String,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val kommunenummer: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
    val gyldighetsperiode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonDetaljer(
    val enhetstyper: List<EregEnhetstype>?,
    val naeringer: List<EregNaering>?,
    val ansatte: List<EregAnsatte>?,
    val forretningsadresser: List<EregAdresse>?,
    val postadresser: List<EregAdresse>?,
    val internettadresser: List<EregNettAdresse>?,
)

class EregEnhetstype (
    val enhetstype: String?,
    val gyldighetsperiode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
class EregNettAdresse (
    val adresse: String?,
    val gyldighetsperiode: GyldighetsPeriode?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class EregNaering(
    val naeringskode: String?,
    val gyldighetsperiode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregAnsatte(
    val antall: Int?,
    val gyldighetsperiode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjon(
    val organisasjonsnummer: String,
    val navn: EregNavn,
    val organisasjonDetaljer: EregOrganisasjonDetaljer,
    val type: String,
    val inngaarIJuridiskEnheter: List<EregEnhetsRelasjon>?,
    val bestaarAvOrganisasjonsledd: List<OrganisasjonsLedd>?
) {
    companion object {
        fun EregOrganisasjon.orgnummerTilOverenhet(): String? =
            if (type == "JuridiskEnhet") {
                null
            } else {
                val juridiskOrgnummer = inngaarIJuridiskEnheter?.firstOrNull()?.organisasjonsnummer
                val orgleddOrgnummer = bestaarAvOrganisasjonsledd?.firstOrNull()?.organisasjonsledd?.organisasjonsnummer
                orgleddOrgnummer ?: juridiskOrgnummer
            }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregEnhetsRelasjon (
    val organisasjonsnummer: String,
    val gyldighetsperiode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrganisasjonsLedd (
    val organisasjonsledd: EregEnhetsRelasjon?,
    val gyldighetsperiode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregNavn(
    val sammensattnavn: String,
    val gyldighetsperiode: GyldighetsPeriode?
)

data class GyldighetsPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?
) {
    companion object {
        fun GyldighetsPeriode?.erGyldig(): Boolean {
            if (this == null) return true
            val now = LocalDate.now()
            return (this.fom == null || fom.isBefore(now)) && (this.tom == null || this.tom.isAfter(now))

        }
    }
}

// generert fra https://snl.no/ISO_3166. Bruker Alfa-2 som key
val landkodeTilLand: Map<String, String> = mapOf(
    "AD" to "Andorra",
    "AE" to "Dei sameinte arabiske emirata",
    "AF" to "Afghanistan",
    "AG" to "Antigua og Barbuda",
    "AI" to "Anguilla (del av Storbritannia)",
    "AL" to "Albania",
    "AM" to "Armenia",
    "AO" to "Angola",
    "AQ" to "Antarktika",
    "AR" to "Argentina",
    "AS" to "Amerikansk Samoa (del av USA)",
    "AT" to "Austerrike",
    "AU" to "Australia",
    "AW" to "Aruba (del av Nederland)",
    "AX" to "Åland (del av Finland)",
    "AZ" to "Aserbajdsjan",
    "BA" to "Bosnia-Hercegovina",
    "BB" to "Barbados",
    "BD" to "Bangladesh",
    "BE" to "Belgia",
    "BF" to "Burkina Faso",
    "BG" to "Bulgaria",
    "BH" to "Bahrain",
    "BI" to "Burundi",
    "BJ" to "Benin",
    "BL" to "St. Barthélemy (del av Frankrike)",
    "BM" to "Bermuda (del av Storbritannia)",
    "BN" to "Brunei",
    "BO" to "Bolivia",
    "BQ" to "Karibisk Nederland (del av Nederland)",
    "BR" to "Brasil",
    "BS" to "Bahamas",
    "BT" to "Bhutan",
    "BV" to "Bouvetøya (del av Norge)",
    "BW" to "Botswana",
    "BY" to "Belarus",
    "BZ" to "Belize",
    "CA" to "Canada",
    "CC" to "Kokosøyane (del av Australia)",
    "CD" to "Kongo-Kinshasa",
    "CF" to "Den sentralafrikanske republikken",
    "CG" to "Kongo-Brazzaville",
    "CH" to "Sveits",
    "CI" to "Elfenbeinskysten",
    "CK" to "Cookøyane (del av New Zealand)",
    "CL" to "Chile",
    "CM" to "Kamerun",
    "CN" to "Kina",
    "CO" to "Colombia",
    "CR" to "Costa Rica",
    "CU" to "Cuba",
    "CV" to "Kapp Verde",
    "CW" to "Curaçao (del av Nederland)",
    "CX" to "Christmasøya (del av Australia)",
    "CY" to "Kypros",
    "CZ" to "Tsjekkia",
    "DE" to "Tyskland",
    "DJ" to "Djibouti",
    "DK" to "Danmark",
    "DM" to "Dominica",
    "DO" to "Den dominikanske republikken",
    "DZ" to "Algerie",
    "EC" to "Ecuador",
    "EE" to "Estland",
    "EG" to "Egypt",
    "EH" to "Vest-Sahara",
    "ER" to "Eritrea",
    "ES" to "Spania",
    "ET" to "Etiopia",
    "FI" to "Finland",
    "FJ" to "Fiji",
    "FK" to "Falklandsøyane (del av Storbritannia)",
    "FM" to "Mikronesiaføderasjonen",
    "FO" to "Færøyane (del av Danmark)",
    "FR" to "Frankrike",
    "GA" to "Gabon",
    "GB" to "Storbritannia",
    "GD" to "Grenada",
    "GE" to "Georgia",
    "GF" to "Fransk Guyana (del av Frankrike)",
    "GG" to "Guernsey (del av Storbritannia)",
    "GH" to "Ghana",
    "GI" to "Gibraltar (del av Storbritannia)",
    "GL" to "Grønland (del av Danmark)",
    "GM" to "Gambia",
    "GN" to "Guinea",
    "GP" to "Guadeloupe (del av Frankrike)",
    "GQ" to "Ekvatorial-Guinea",
    "GR" to "Hellas",
    "GS" to "Sør-Georgia og Sør-Sandwichøyane (del av Storbritannia)",
    "GT" to "Guatemala",
    "GU" to "Guam (del av USA)",
    "GW" to "Guinea-Bissau",
    "GY" to "Guyana",
    "HK" to "Hongkong (del av Kina)",
    "HM" to "Heard- og McDonald-øyane (del av Australia)",
    "HN" to "Honduras",
    "HR" to "Kroatia",
    "HT" to "Haiti",
    "HU" to "Ungarn",
    "ID" to "Indonesia",
    "IE" to "Irland",
    "IL" to "Israel",
    "IM" to "Man (del av Storbritannia)",
    "IN" to "India",
    "IO" to "Britisk territorium i Indiahavet (del av Storbritannia)",
    "IQ" to "Irak",
    "IR" to "Iran",
    "IS" to "Island",
    "IT" to "Italia",
    "JE" to "Jersey (del av Storbritannia)",
    "JM" to "Jamaica",
    "JO" to "Jordan",
    "JP" to "Japan",
    "KE" to "Kenya",
    "KG" to "Kirgisistan",
    "KH" to "Kambodsja",
    "KI" to "Kiribati",
    "KM" to "Komorane",
    "KN" to "St. Kitts og Nevis",
    "KP" to "Nord-Korea",
    "KR" to "Sør-Korea",
    "KW" to "Kuwait",
    "KY" to "Caymanøyane (del av Storbritannia)",
    "KZ" to "Kasakhstan",
    "LA" to "Laos",
    "LB" to "Libanon",
    "LC" to "St. Lucia",
    "LI" to "Liechtenstein",
    "LK" to "Sri Lanka",
    "LR" to "Liberia",
    "LS" to "Lesotho",
    "LT" to "Litauen",
    "LU" to "Luxembourg",
    "LV" to "Latvia",
    "LY" to "Libya",
    "MA" to "Marokko",
    "MC" to "Monaco",
    "MD" to "Moldova",
    "ME" to "Montenegro",
    "MF" to "St. Martin (del av Frankrike)",
    "MG" to "Madagaskar",
    "MH" to "Marshalløyane",
    "MK" to "Nord-Makedonia",
    "ML" to "Mali",
    "MM" to "Myanmar",
    "MN" to "Mongolia",
    "MO" to "Macao (del av Kina)",
    "MP" to "Nord-Marianane (del av USA)",
    "MQ" to "Martinique (del av Frankrike)",
    "MR" to "Mauritania",
    "MS" to "Montserrat (del av Storbritannia)",
    "MT" to "Malta",
    "MU" to "Mauritius",
    "MV" to "Maldivane",
    "MW" to "Malawi",
    "MX" to "Mexico",
    "MY" to "Malaysia",
    "MZ" to "Mosambik",
    "NA" to "Namibia",
    "NC" to "Ny-Caledonia (del av Frankrike)",
    "NE" to "Niger",
    "NF" to "Norfolkøya (del av Australia)",
    "NG" to "Nigeria",
    "NI" to "Nicaragua",
    "NL" to "Nederland",
    "NO" to "Norge",
    "NP" to "Nepal",
    "NR" to "Nauru",
    "NU" to "Niue (del av New Zealand)",
    "NZ" to "New Zealand",
    "OM" to "Oman",
    "PA" to "Panama",
    "PE" to "Peru",
    "PF" to "Fransk Polynesia (del av Frankrike)",
    "PG" to "Papua Ny-Guinea",
    "PH" to "Filippinane",
    "PK" to "Pakistan",
    "PL" to "Polen",
    "PM" to "St. Pierre og Miquelon (del av Frankrike)",
    "PN" to "Pitcairn (del av Storbritannia)",
    "PR" to "Puerto Rico (del av USA)",
    "PS" to "Palestina",
    "PT" to "Portugal",
    "PW" to "Palau",
    "PY" to "Paraguay",
    "QA" to "Qatar",
    "RE" to "Réunion (del av Frankrike)",
    "RO" to "Romania",
    "RS" to "Serbia",
    "RU" to "Russland",
    "RW" to "Rwanda",
    "SA" to "Saudi-Arabia",
    "SB" to "Salomonøyane",
    "SC" to "Seychellane",
    "SD" to "Sudan",
    "SE" to "Sverige",
    "SG" to "Singapore",
    "SH" to "St. Helena, Ascension og Tristan da Cunha (del av Storbritannia)",
    "SI" to "Slovenia",
    "SJ" to "Svalbard og Jan Mayen (del av Norge)",
    "SK" to "Slovakia",
    "SL" to "Sierra Leone",
    "SM" to "San Marino",
    "SN" to "Senegal",
    "SO" to "Somalia",
    "SR" to "Surinam",
    "SS" to "Sør-Sudan",
    "ST" to "São Tomé og Príncipe",
    "SV" to "El Salvador",
    "SX" to "Sint Maarten (del av Nederland)",
    "SY" to "Syria",
    "SZ" to "Eswatini",
    "TC" to "Turks- og Caicosøyane (del av Storbritannia)",
    "TD" to "Tsjad",
    "TF" to "Dei franske sørterritoria (del av Frankrike)",
    "TG" to "Togo",
    "TH" to "Thailand",
    "TJ" to "Tadsjikistan",
    "TK" to "Tokelau (del av New Zealand)",
    "TL" to "Aust-Timor",
    "TM" to "Turkmenistan",
    "TN" to "Tunisia",
    "TO" to "Tonga",
    "TR" to "Tyrkia",
    "TT" to "Trinidad og Tobago",
    "TV" to "Tuvalu",
    "TW" to "Taiwan",
    "TZ" to "Tanzania",
    "UA" to "Ukraina",
    "UG" to "Uganda",
    "UM" to "USAs ytre småøyar (del av USA)",
    "US" to "USA",
    "UY" to "Uruguay",
    "UZ" to "Usbekistan",
    "VA" to "Vatikanstaten",
    "VC" to "St. Vincent og Grenadinane",
    "VE" to "Venezuela",
    "VG" to "Jomfruøyane (Britisk) (del av Storbritannia)",
    "VI" to "Jomfruøyane (USA) (del av USA)",
    "VN" to "Vietnam",
    "VU" to "Vanuatu",
    "WF" to "Wallis- og Futunaøyane (del av Frankrike)",
    "WS" to "Samoa",
    "YE" to "Jemen",
    "YT" to "Mayotte (del av Frankrike)",
    "ZA" to "Sør-Afrika",
    "ZM" to "Zambia",
    "ZW" to "Zimbabwe",
)
