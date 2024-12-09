package no.nav.arbeidsgiver.min_side.services.ereg

import com.fasterxml.jackson.databind.JsonNode
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import no.nav.arbeidsgiver.min_side.config.callIdIntercetor
import no.nav.arbeidsgiver.min_side.models.Organisasjon
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
    fun hentUnderenhet(virksomhetsnummer: String): Organisasjon? {
        return try {
            val json = restTemplate.getForEntity(
                "/v1/organisasjon/{virksomhetsnummer}?inkluderHierarki=true",
                JsonNode::class.java,
                mapOf("virksomhetsnummer" to virksomhetsnummer)
            ).body
            underenhet(json)
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    @Cacheable(EREG_CACHE_NAME)
    fun hentOverenhet(orgnummer: String): Organisasjon? {
        return try {
            val json = restTemplate.getForEntity(
                "/v1/organisasjon/{orgnummer}",
                JsonNode::class.java,
                mapOf("orgnummer" to orgnummer)
            ).body
            overenhet(json)
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    fun underenhet(json: JsonNode?): Organisasjon? {
        if (json == null || json.isEmpty) {
            return null
        }
        return Organisasjon(
            name = samletNavn(json),
            parentOrganizationNumber = orgnummerTilOverenhet(json),
            organizationNumber = json.at("/organisasjonsnummer").asText(),
            organizationForm = json.at("/organisasjonDetaljer/enhetstyper/0/enhetstype").asText(),
        )
    }


    fun overenhet(json: JsonNode?): Organisasjon? {
        if (json == null || json.isEmpty) {
            return null
        }
        return Organisasjon(
            name = samletNavn(json),
            parentOrganizationNumber = orgnummerTilOverenhet(json),
            organizationNumber = json.at("/organisasjonsnummer").asText(),
            organizationForm = json.at("/organisasjonDetaljer/enhetstyper/0/enhetstype").asText(),
        )
    }

    private fun orgnummerTilOverenhet(json: JsonNode): String? =
        if ("JuridiskEnhet" == json.at("/type").asText()) {
            null
        } else {
            val juridiskOrgnummer = json.at("/inngaarIJuridiskEnheter/0/organisasjonsnummer").asText(null)?.ifBlank { null }
            val orgleddOrgnummer = json.at("/bestaarAvOrganisasjonsledd/0/organisasjonsledd/organisasjonsnummer").asText(null)?.ifBlank { null }
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