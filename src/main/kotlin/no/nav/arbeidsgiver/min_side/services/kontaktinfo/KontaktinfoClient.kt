package no.nav.arbeidsgiver.min_side.services.kontaktinfo

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import no.nav.arbeidsgiver.min_side.infrastruktur.MaskinportenTokenProvider
import no.nav.arbeidsgiver.min_side.infrastruktur.Miljø
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient.Kontaktinfo

interface KontaktinfoClient {
    suspend fun hentKontaktinfo(orgnr: String): Kontaktinfo

    data class Kontaktinfo(
        val eposter: Set<String>,
        val telefonnumre: Set<String>,
    ) {
        val harKontaktinfo: Boolean
            get() = eposter.isNotEmpty() || telefonnumre.isNotEmpty()
    }
}

class KontaktinfoClientImpl(
    private val httpClient: HttpClient,
    private val tokenProvider: MaskinportenTokenProvider,
) : KontaktinfoClient {
    companion object {
        val ingress = Miljø.Altinn.baseUrl
        val altinnApiKey = Miljø.Altinn.altinnHeader
        val targetScope = "altinn:serviceowner/organizations"
        val targetResource = Miljø.resolve(
            prod = { "https://www.altinn.no/" },
            other = { "https://tt02.altinn.no/" }
        )
    }




    override suspend fun hentKontaktinfo(orgnr: String): Kontaktinfo {
        val officialContactsResponse = httpClient.get {
            url {
                takeFrom(ingress)
                path("/api/serviceowner/organizations/${orgnr}/officialcontacts")
            }
            header("apiKey", altinnApiKey)
            bearerAuth(
                tokenProvider.token(targetScope, mapOf("resource" to targetResource)).fold(
                    onSuccess = { it.accessToken },
                    onError = { throw Exception("Failed to fetch token: ${it.status} ${it.error}") }
                )
            )
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

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonIgnoreUnknownKeys
    private class ContactInfoDTO(
        @SerialName("MobileNumber")
        val mobileNumber: String,
        @SerialName("EMailAddress")
        val emailAddress: String,
    )
}