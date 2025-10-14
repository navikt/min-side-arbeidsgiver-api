package no.nav.arbeidsgiver.min_side.tilgangsstyring

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.config.Miljø
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService

class AltinnRollerClient(
    private val maskinportenTokenService: MaskinportenTokenService,
) {
    private val altinnApiBaseUrl = Miljø.Altinn.baseUrl
    private val altinnApiKey = Miljø.Altinn.altinnHeader

    private val client = defaultHttpClient()

    private val safeRoleName = Regex("^[A-ZÆØÅ]+$")
    private val orgnrRegex = Regex("^[0-9]{9}$")

    suspend fun harAltinnRolle(
        fnr: String,
        orgnr: String,
        altinnRoller: Set<String>,
        externalRoller: Set<String>,
    ): Boolean {
        require(orgnr.matches(orgnrRegex)) // user-controlled, so ensure only digits before injecting into query
        require(altinnRoller.isNotEmpty() && externalRoller.isNotEmpty()) {
            "skrevet under antagelse om at både altinnRoller og externalRoller er non-empty"
        }

        fun roleDefintionFilter(roller: Iterable<String>) =
            roller.joinToString(separator = "+or+") {
                require(it.matches(safeRoleName))
                "RoleDefinitionCode+eq+'$it'"
            }

        val altinnRolleFilter = roleDefintionFilter(altinnRoller)
        val eregRolleFilter = roleDefintionFilter(externalRoller)
        val filter =
            "(RoleType+eq+'Altinn'+and+($altinnRolleFilter))+or+(RoleType+eq+'External'+and+($eregRolleFilter))"

        val rollerResponse =
            client.get("$altinnApiBaseUrl/api/serviceowner/authorization/roles?subject=$fnr&reportee=$orgnr&${'$'}filter=$filter") {
                header("apikey", altinnApiKey)
                bearerAuth(maskinportenTokenService.currentAccessToken())
            }

        val roller = when (rollerResponse.status) {
            HttpStatusCode.OK -> rollerResponse.body<List<RoleDTO>?>()
                ?: throw RuntimeException("serviceowner/authorization/roles missing body")

            HttpStatusCode.BadRequest -> {
                val body = rollerResponse.body<String>()
                if (body.contains("User profile")) { // Altinn returns 400 if user does not exist
                    emptyList()
                } else {
                    throw RuntimeException("serviceowner/authorization/roles BadRequest: $body")
                }
            }

            else -> throw RuntimeException("serviceowner/authorization/roles unexpected status: ${rollerResponse.status}")
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
}