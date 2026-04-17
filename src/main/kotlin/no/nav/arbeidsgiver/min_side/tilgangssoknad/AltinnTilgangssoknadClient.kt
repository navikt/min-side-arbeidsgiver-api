package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.AltinnPlattformTokenClient
import no.nav.arbeidsgiver.min_side.infrastruktur.Miljø
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson

interface AltinnTilgangssoknadClient {
    suspend fun opprettDelegationRequest(fnr: String, request: CreateDelegationRequest): DelegationRequestResponse
    suspend fun hentDelegationRequestStatus(id: String): DelegationRequestStatus

    companion object {
        const val API_PATH = "/accessmanagement/api/v1/serviceowner/delegationrequests"
        const val SCOPE_READ = "altinn:serviceowner/delegationrequests.read"
        const val SCOPE_WRITE = "altinn:serviceowner/delegationrequests.write"
        const val NAV_RESOURCE_PREFIX = "nav_"
    }
}

class AltinnTilgangssoknadClientImpl(
    defaultHttpClient: HttpClient,
    private val plattformTokenClient: AltinnPlattformTokenClient,
    private val altinnPlatformBaseUrl: String = Miljø.Altinn.platformBaseUrl,
) : AltinnTilgangssoknadClient {

    private val httpClient = defaultHttpClient.config {
        expectSuccess = true

        install(ContentNegotiation) {
            json(defaultJson)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun opprettDelegationRequest(
        fnr: String,
        request: CreateDelegationRequest,
    ): DelegationRequestResponse {
        val token = plattformTokenClient.token(AltinnTilgangssoknadClient.SCOPE_WRITE)
        return httpClient.post {
            url {
                takeFrom(altinnPlatformBaseUrl)
                path(AltinnTilgangssoknadClient.API_PATH)
            }
            bearerAuth(token)
            setBody(request.toServiceOwnerRequest(fnr))
        }.body()
    }

    override suspend fun hentDelegationRequestStatus(id: String): DelegationRequestStatus {
        val token = plattformTokenClient.token(AltinnTilgangssoknadClient.SCOPE_READ)
        return httpClient.get {
            url {
                takeFrom(altinnPlatformBaseUrl)
                path("${AltinnTilgangssoknadClient.API_PATH}/$id/status")
            }
            bearerAuth(token)
        }.body()
    }
}


/**
 * User-facing request body. `from` is derived from the logged-in user's fnr.
 */
@Serializable
data class CreateDelegationRequest(
    val to: String? = null,
    val resource: RequestReferenceDto? = null,
    @SerialName("package")
    val accessPackage: RequestReferenceDto? = null,
) {
    fun toServiceOwnerRequest(fnr: String) = CreateServiceOwnerRequest(
        from = "urn:altinn:person:identifier-no:$fnr",
        to = to,
        resource = resource,
        accessPackage = accessPackage,
    )
}

/**
 * Wire-level request body for Altinn serviceowner delegationrequests API.
 */
@Serializable
data class CreateServiceOwnerRequest(
    val from: String? = null,
    val to: String? = null,
    val resource: RequestReferenceDto? = null,
    @SerialName("package")
    val accessPackage: RequestReferenceDto? = null,
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
    @SerialName("package")
    val accessPackage: RequestReferenceDto? = null,
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
