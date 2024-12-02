package no.nav.arbeidsgiver.min_side.tilgangsstyring

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.arbeidsgiver.min_side.clients.retryInterceptor
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException.BadRequest
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

@Component
class AltinnRollerClient(
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

    private val safeRoleName = Regex("^[A-ZÆØÅ]+$")
    private val orgnrRegex = Regex("^[0-9]{9}$")

    fun harAltinnRolle(
        fnr: String,
        orgnr: String,
        altinnRoller: Set<String>,
        externalRoller: Set<String>,
    ): Boolean {
        require(orgnr.matches(orgnrRegex)) // user-controlled, so ensure only digits before injecting into query
        require(altinnRoller.isNotEmpty() && externalRoller.isNotEmpty()) {
            "skrevet under antagelse om at både altinnRoller og externalRoller er non-empty"
        }

        val headers = HttpHeaders().apply {
            set("apikey", altinnApiKey)
            setBearerAuth(maskinportenTokenService.currentAccessToken())
        }

        fun roleDefintionFilter(roller: Iterable<String>) =
            roller.joinToString(separator = "+or+") {
                require(it.matches(safeRoleName))
                "RoleDefinitionCode+eq+'$it'"
            }

        val altinnRolleFilter = roleDefintionFilter(altinnRoller)
        val eregRolleFilter = roleDefintionFilter(externalRoller)
        val filter = "(RoleType+eq+'Altinn'+and+($altinnRolleFilter))+or+(RoleType+eq+'External'+and+($eregRolleFilter))"

        val roller = try {
            restTemplate.exchange(
                "/api/serviceowner/authorization/roles?subject={subject}&reportee={reportee}&${'$'}filter={filter}&ForceEIAuthentication",
                GET,
                HttpEntity<Nothing>(headers),
                roleListType,
                mapOf<String, Any>(
                    "subject" to fnr,
                    "reportee" to orgnr,
                    "filter" to filter,
                )
            ).body ?: throw RuntimeException("serviceowner/authorization/roles missing body")
        } catch (e: BadRequest) {
            if (e.message!!.contains("User profile")) { // Altinn returns 400 if user does not exist
                emptyList()
            } else {
                throw e
            }
        }

        /* Kanskje litt paranoid, men da er vi korrekte uavhengig av om $filter er implementert
         * som forventet hos altinn eller om vi gjør noe feil med filteret. */
        return roller.any { it.roleType == "Altinn" && it.roleDefinitionCode in altinnRoller }
                || roller.any { it.roleType == "External" && it.roleDefinitionCode in externalRoller }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class RoleDTO(
        @JsonProperty("RoleType") val roleType: String,
        @JsonProperty("RoleDefinitionCode") val roleDefinitionCode: String,
    )

    companion object {
        private val roleListType = object : ParameterizedTypeReference<List<RoleDTO>>() {}
    }
}