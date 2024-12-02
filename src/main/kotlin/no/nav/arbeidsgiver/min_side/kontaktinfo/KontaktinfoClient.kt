package no.nav.arbeidsgiver.min_side.kontaktinfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Component
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException
import kotlin.collections.List

@Component
class KontaktinfoClient(
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${altinn.apiBaseUrl}") altinnApiBaseUrl: String,
    @Value("\${altinn.altinnHeader}") private val altinnApiKey: String,
    private val maskinportenTokenService: MaskinportenTokenService,
) {
    private val restTemplate = restTemplateBuilder
        .rootUri(altinnApiBaseUrl)
        .additionalInterceptors(
            retryInterceptor(
                3,
                250L,
                SocketException::class.java,
                SSLHandshakeException::class.java,
            )
        )
        .build()

    data class Kontaktinfo(
        val eposter: Set<String>,
        val telefonnumre: Set<String>,
    ) {
        val harKontaktinfo: Boolean
            get() = eposter.isNotEmpty() || telefonnumre.isNotEmpty()
    }

    fun hentKontaktinfo(orgnr: String): Kontaktinfo {
        val headers = HttpHeaders().apply {
            set("apikey", altinnApiKey)
            setBearerAuth(maskinportenTokenService.currentAccessToken())
        }

        val officialcontacts = restTemplate.exchange(
            "/api/serviceowner/organizations/{organizationNumber}/officialcontacts?ForceEIAuthentication",
            GET,
            HttpEntity<Nothing>(headers),
            contactInfoListType,
            mapOf(
                "organizationNumber" to orgnr
            )
        ).body ?: throw RuntimeException("serviceowner/organizations/{orgnr}/officialcontacts got null body")

        /* Ved ukjent orgnr returnerer altinn:
         * HTTP/1.1 400 Invalid organization number: 0000000
         **/

        val eposter = mutableSetOf<String>()
        val telefonnumre = mutableSetOf<String>()

        for (contact in officialcontacts) {
            if (contact.emailAddress.isNotBlank()) {
                eposter.add(contact.emailAddress)
            }
            if (contact.mobileNumber.isNotBlank()) {
                telefonnumre.add(contact.mobileNumber)
            }
        }

        return Kontaktinfo(
            eposter = eposter,
            telefonnumre = telefonnumre,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class ContactInfoDTO(
        @JsonProperty("MobileNumber")
        val mobileNumber: String,
        @JsonProperty("EMailAddress")
        val emailAddress: String,
    )

    companion object {
        private val contactInfoListType = object : ParameterizedTypeReference<List<ContactInfoDTO>>() {}
    }
}