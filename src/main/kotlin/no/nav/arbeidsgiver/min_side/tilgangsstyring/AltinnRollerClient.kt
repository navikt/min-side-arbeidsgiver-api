package no.nav.arbeidsgiver.min_side.tilgangsstyring

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.MaskinportenTokenProvider
import no.nav.arbeidsgiver.min_side.infrastruktur.Miljø
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient.Companion.altinnApiKey
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient.Companion.apiPath
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient.Companion.ingress
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient.Companion.targetResource
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient.Companion.targetScope

interface AltinnRollerClient {
    suspend fun harAltinnRolle(
        fnr: String,
        orgnr: String,
        altinnRoller: Set<String>,
        externalRoller: Set<String>,
    ): Boolean

    companion object {
        val ingress = Miljø.Altinn.baseUrl
        val apiPath = "/api/serviceowner/authorization/roles"
        val altinnApiKey = Miljø.Altinn.altinnHeader
        val targetScope = "altinn:serviceowner/rolesandrights"
        val targetResource = Miljø.resolve(
            prod = { "https://www.altinn.no/" },
            other = { "https://tt02.altinn.no/" }
        )
    }
}

class AltinnRollerClientImpl(
    defaultHttpClient: HttpClient,
    private val tokenProvider: MaskinportenTokenProvider,
) : AltinnRollerClient {

    private val httpClient = defaultHttpClient.config {
        install(ContentNegotiation) {
            json(defaultJson)
        }
    }

    private val safeRoleName = Regex("^[A-ZÆØÅ]+$")
    private val orgnrRegex = Regex("^[0-9]{9}$")

    override suspend fun harAltinnRolle(
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
            httpClient.get {
                url {
                    takeFrom(ingress)
                    path(apiPath)
                    parameters.append("subject", fnr)
                    parameters.append("reportee", orgnr)
                    parameters.append($$"$filter", filter)
                }
                header("apikey", altinnApiKey)
                bearerAuth(tokenProvider.token(targetScope, mapOf("resource" to targetResource)).fold(
                    onSuccess = { it.accessToken },
                    onError = { throw Exception("Failed to fetch token: ${it.status} ${it.error}") }
                ))
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

    @Serializable
    private class RoleDTO(
        @SerialName("RoleType") val roleType: String,
        @SerialName("RoleDefinitionCode") val roleDefinitionCode: String,
    )
}