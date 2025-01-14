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
    private val restTemplate = restTemplateBuilder
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
            restTemplate.getForEntity(
                "/v1/organisasjon/{virksomhetsnummer}?inkluderHierarki=true",
                EregOrganisasjon::class.java,
                mapOf("virksomhetsnummer" to virksomhetsnummer)
            ).body
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
                "/v1/organisasjon/{orgnummer}", //TODO: endre til v2
                EregOrganisasjon::class.java,
                mapOf("orgnummer" to orgnummer)
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
    val adresse: String?,
    val kommune: String?,
    val kommunenummer: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
    val gyldighetsPeriode: GyldighetsPeriode?
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
    val gyldighetsPeriode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
class EregNettAdresse (
    val adresse: String?,
    val gyldighetsPeriode: GyldighetsPeriode?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class EregNaering(
    val naeringskode: String?,
    val gyldighetsPeriode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregAnsatte(
    val antall: Int?,
    val gyldighetsPeriode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjon(
    val organisasjonsnummer: String,
    val navn: EregNavn,
    val organisasjonsDetaljer: EregOrganisasjonDetaljer,
    val type: String,
    val ingaarIJuridiskEnheter: List<EregEnhetsRelasjon>?,
    val bestaarAvOrganisasjonsledd: List<EregEnhetsRelasjon>?
) {
    companion object {
        fun EregOrganisasjon.orgnummerTilOverenhet(): String? =
            if (type == "JuridiskEnhet") {
                null
            } else {
                val juridiskOrgnummer = ingaarIJuridiskEnheter?.firstOrNull()?.organisasjonsnummer
                val orgleddOrgnummer = bestaarAvOrganisasjonsledd?.firstOrNull()?.organisasjonsnummer
                orgleddOrgnummer ?: juridiskOrgnummer
            }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregEnhetsRelasjon (
    val organisasjonsnummer: String,
    val gyldighetsPeriode: GyldighetsPeriode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregNavn(
    val sammensattnavn: String,
    val gyldighetsPeriode: GyldighetsPeriode?
)

data class Kode(
    val kode: String?,
    val beskrivelse: String?
)

data class GyldighetsPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?
) {
    companion object {
        fun GyldighetsPeriode?.erGyldig(): Boolean {
            if (this == null) return true
            val now = LocalDate.now()
            return (this.fom == null || fom.isBefore(now) && (this.tom == null || this.tom.isAfter(now)))

        }
    }
}