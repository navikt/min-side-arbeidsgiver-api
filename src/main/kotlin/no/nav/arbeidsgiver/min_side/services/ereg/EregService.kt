package no.nav.arbeidsgiver.min_side.services.ereg

import com.fasterxml.jackson.databind.JsonNode
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
import java.util.*
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
            val json = restTemplate.getForEntity(
                "/v1/organisasjon/{virksomhetsnummer}?inkluderHierarki=true",
                JsonNode::class.java,
                mapOf("virksomhetsnummer" to virksomhetsnummer)
            ).body
            eregOrganisasjon(json)
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
            val json = restTemplate.getForEntity(
                "/v1/organisasjon/{orgnummer}",
                JsonNode::class.java,
                mapOf("orgnummer" to orgnummer)
            ).body
            eregOrganisasjon(json)
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    private fun naeringsKoder(json: JsonNode): List<Kode> {
        return json.at("/organisasjonDetaljer/naeringer").map { //TODO: gyldighet?
            Kode(it.at("/naeringskode").asText(), it.at("/description").asText())
        }
    }

    private fun adresse(json: JsonNode): Adresse? {
        if (!json.isMissingNode)
            return Adresse(
                adresse = json.at("/adresselinje1").asText() + json.at("/adresselinje2").asText() + json.at("/adresselinje3").asText(),
                kommune = json.at("/kommune").asText(),
                kommunenummer = json.at("/kommunenummer").asText(),
                land = json.at("/land").asText(), //TODO: denne mangler fra responsen
                landkode = json.at("/landkode").asText(),
                postnummer = json.at("/postnummer").asText(),
                poststed = json.at("/poststed").asText(),
                type = json.at("/type").asText()
            )
        return null
    }

    private fun organisasjonsform(json: JsonNode): String {
        return json.at("/organisasjonDetaljer/enhetstyper/0/enhetstype").asText()
    }

    private fun internettAdresse(json: JsonNode): String {
        return json.at("/organisasjonDetaljer/internettadresser/0/adresse").asText()
    }

    private fun ansatte(json: JsonNode): Int? {
        return json.at("/organisasjonDetaljer/ansatte/0/antall").asText("").toIntOrNull()
    }

    fun eregOrganisasjon(json: JsonNode?): EregOrganisasjon? {
        if (json == null || json.isEmpty) {
            return null
        }
        return EregOrganisasjon(
            organisasjonsnummer = json.at("/organisasjonsnummer").asText(),
            navn = samletNavn(json),
            organisasjonsform = organisasjonsform(json),
            overordnetEnhet = orgnummerTilOverenhet(json),
            naeringskoder = naeringsKoder(json),
            postadresse = adresse(json.at("/organisasjonDetaljer/postadresser/0")),
            forretningsadresse = adresse(json.at("/organisasjonDetaljer/forretningsadresser/0")),
            hjemmeside = internettAdresse(json),
            antallAnsatte = ansatte(json)
        )
    }

    private fun orgnummerTilOverenhet(json: JsonNode): String? =
        if ("JuridiskEnhet" == json.at("/type").asText()) {
            null
        } else {
            val juridiskOrgnummer =
                json.at("/inngaarIJuridiskEnheter/0/organisasjonsnummer").asText(null)?.ifBlank { null }
            val orgleddOrgnummer =
                json.at("/bestaarAvOrganisasjonsledd/0/organisasjonsledd/organisasjonsnummer").asText(null)
                    ?.ifBlank { null }
            orgleddOrgnummer ?: juridiskOrgnummer
        }

    companion object {
        private fun samletNavn(json: JsonNode) = listOf(
            json.at("/navn/navnelinje1").asText(null),
            json.at("/navn/navnelinje2").asText(null),
            json.at("/navn/navnelinje3").asText(null),
            json.at("/navn/navnelinje4").asText(null),
            json.at("/navn/navnelinje5").asText(null),
        )
            .filter(Objects::nonNull)
            .joinToString(" ")
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