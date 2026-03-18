package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService.Companion.audience
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService.Companion.ingress

interface AltinnTilgangssoknadClient {
    suspend fun opprettDelegationRequest(token: String, request: CreateDelegationRequest): DelegationRequestResponse
    suspend fun hentDelegationRequestStatus(token: String, id: String): DelegationRequestStatus
}

class AltinnTilgangssoknadClientImpl(
    defaultHttpClient: HttpClient,
    private val tokenExchanger: TokenXTokenExchanger,
) : AltinnTilgangssoknadClient {

    private val httpClient = defaultHttpClient.config {
        expectSuccess = true

        install(ContentNegotiation) {
            json(defaultJson)
        }

        install(HttpTimeout) {
            this.requestTimeoutMillis = 30_000
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    private suspend fun exchangeToken(userToken: String): String =
        tokenExchanger.exchange(
            target = audience,
            userToken = userToken,
        ).fold(
            onSuccess = { it.accessToken },
            onError = { throw Exception("Failed to exchange token: ${it.error}") }
        )

    override suspend fun opprettDelegationRequest(token: String, request: CreateDelegationRequest): DelegationRequestResponse {
        val oboToken = exchangeToken(token)
        return httpClient.post {
            url {
                takeFrom(ingress)
                path("/delegation-request")
            }
            bearerAuth(oboToken)
            setBody(request)
        }.body()
    }

    override suspend fun hentDelegationRequestStatus(token: String, id: String): DelegationRequestStatus {
        val oboToken = exchangeToken(token)
        return httpClient.get {
            url {
                takeFrom(ingress)
                path("/delegation-request/$id/status")
            }
            bearerAuth(oboToken)
        }.body()
    }
}


@Serializable
data class CreateDelegationRequest(
    val to: String,
    val resource: RequestReferenceDto? = null,
    @Suppress("PropertyName")
    val `package`: RequestReferenceDto? = null,
)

@Serializable
data class RequestReferenceDto(
    val id: String? = null,
    val referenceId: String? = null,
)

@Serializable
data class PartyEntityDto(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val variant: String? = null,
    val organizationIdentifier: String? = null,
    val personIdentifier: String? = null,
)

@Serializable
data class RequestLinksDto(
    val detailsLink: String? = null,
    val statusLink: String? = null,
)

@Serializable
data class DelegationRequestResponse(
    val id: String? = null,
    val status: DelegationRequestStatus? = null,
    val type: String? = null,
    val lastUpdated: String? = null,
    val resource: RequestReferenceDto? = null,
    @Suppress("PropertyName")
    val `package`: RequestReferenceDto? = null,
    val links: RequestLinksDto? = null,
    val from: PartyEntityDto? = null,
    val to: PartyEntityDto? = null,
)

@Serializable
enum class DelegationRequestStatus {
    None,
    Draft,
    Pending,
    Approved,
    Rejected,
    Withdrawn,
}
