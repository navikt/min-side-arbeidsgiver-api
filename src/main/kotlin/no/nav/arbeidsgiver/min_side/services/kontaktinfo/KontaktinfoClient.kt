package no.nav.arbeidsgiver.min_side.services.kontaktinfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.config.Miljø
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService

class KontaktinfoClient(
    private val maskinportenTokenService: MaskinportenTokenService,
) {
    private val client = defaultHttpClient()
    private val altinnApiBaseUrl = Miljø.Altinn.baseUrl
    private val altinnApiKey = Miljø.Altinn.altinnHeader

    data class Kontaktinfo(
        val eposter: Set<String>,
        val telefonnumre: Set<String>,
    ) {
        val harKontaktinfo: Boolean
            get() = eposter.isNotEmpty() || telefonnumre.isNotEmpty()
    }

    suspend fun hentKontaktinfo(orgnr: String): Kontaktinfo {
        val officialContactsResponse = client.request(
            "$altinnApiBaseUrl/api/serviceowner/organizations/${orgnr}/officialcontacts?ForceEIAuthentication"
        ) {
            method = HttpMethod.Get
            header("apiKey", altinnApiKey)
            bearerAuth(maskinportenTokenService.currentAccessToken())
        }
        if (officialContactsResponse.status != HttpStatusCode.OK) {
            throw RuntimeException("serviceowner/organizations/{$orgnr}/officialcontacts returned ${officialContactsResponse.status}")
        }
        val officialContacts = officialContactsResponse.body<List<ContactInfoDTO>>()

        val eposter = mutableSetOf<String>()
        val telefonnumre = mutableSetOf<String>()

        for (contact in officialContacts) {
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
}