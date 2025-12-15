package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient.Companion.altinnApiKey
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient.Companion.apiPath
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient.Companion.clientJsonConfig
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient.Companion.ingress
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient.Companion.targetResource
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient.Companion.targetScope
import no.nav.arbeidsgiver.min_side.tilgangssoknad.DelegationRequest.RequestResource

interface AltinnTilgangssoknadClient {
    suspend fun hentSøknader(fødselsnummer: String): List<AltinnTilgangssøknad>
    suspend fun sendSøknad(fødselsnummer: String, søknadsskjema: AltinnTilgangssøknadsskjema): AltinnTilgangssøknad

    companion object {
        val ingress = Miljø.Altinn.baseUrl
        val apiPath = "/api/serviceowner/delegationRequests"
        val altinnApiKey = Miljø.Altinn.altinnHeader
        val targetScope = listOf(
            "altinn:serviceowner/delegationrequests.read",
            "altinn:serviceowner/delegationrequests.write"
        ).joinToString(" ")
        val targetResource = Miljø.resolve(
            prod = { "https://www.altinn.no/" },
            other = { "https://tt02.altinn.no/" }
        )
        fun Configuration.clientJsonConfig() {
            json(
                json = Json(defaultJson) {
                    explicitNulls = false
                },
                contentType = ContentType.Application.HalJson
            )
        }
    }
}

class AltinnTilgangssoknadClientImpl(
    private val tokenProvider: MaskinportenTokenProvider,
    private val httpClient: HttpClient = defaultHttpClient {
        install(ContentNegotiation) {
            clientJsonConfig()
        }
    },
) : AltinnTilgangssoknadClient {

    private val log = logger()

    override suspend fun hentSøknader(fødselsnummer: String): List<AltinnTilgangssøknad> {
        var shouldContinue = true
        var continuationtoken: String? = null
        val resultat = ArrayList<AltinnTilgangssøknad>()
        while (shouldContinue) {
            val body = try {
                val response = httpClient.get {
                    url {
                        takeFrom(ingress)
                        path(apiPath)
                        parameter($$"$filter", "CoveredBy eq '$fødselsnummer'")
                        if (continuationtoken != null) {
                            parameter("continuation", continuationtoken)
                        }
                    }
                    header("apikey", altinnApiKey)
                    bearerAuth(
                        tokenProvider.token(targetScope, mapOf("resource" to targetResource)).fold(
                            onSuccess = { it.accessToken },
                            onError = { throw Exception("Failed to fetch token: ${it.status} ${it.error}") }
                        )
                    )
                    accept(ContentType.Application.HalJson)
                    contentType(ContentType.Application.HalJson)
                }
                log.info("Altinn delegation response: ${response.status} ${response.bodyAsText()}")
                response.body<Søknadsstatus?>()
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.BadRequest) {
                    if (e.response.bodyAsText().contains("User profile")) {
                        null
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }


            if (body == null) {
                log.error("Altinn delegation requests: body missing")
                break
            }

            if (body.embedded!!.delegationRequests!!.isEmpty()) {
                shouldContinue = false
            } else {
                continuationtoken = body.continuationtoken
            }

            body.embedded.delegationRequests.mapTo(resultat)
            { søknadDTO: DelegationRequest ->
                AltinnTilgangssøknad(
                    orgnr = søknadDTO.OfferedBy,
                    status = søknadDTO.RequestStatus,
                    createdDateTime = søknadDTO.Created,
                    lastChangedDateTime = søknadDTO.LastChanged,
                    serviceCode = søknadDTO.RequestResources!![0].ServiceCode,
                    serviceEdition = søknadDTO.RequestResources[0].ServiceEditionCode,
                    submitUrl = søknadDTO.links!!.sendRequest!!.href,
                )
            }
        }
        return resultat
    }

    override suspend fun sendSøknad(fødselsnummer: String, søknadsskjema: AltinnTilgangssøknadsskjema): AltinnTilgangssøknad {
        val response = httpClient.post {
            url {
                takeFrom(ingress)
                path(apiPath)
            }
            accept(ContentType.Application.HalJson)
            header("apikey", altinnApiKey)
            contentType(ContentType.Application.HalJson)
            bearerAuth(
                tokenProvider.token(targetScope, mapOf("resource" to targetResource)).fold(
                    onSuccess = { it.accessToken },
                    onError = { throw Exception("Failed to fetch token: ${it.status} ${it.error}") }
                )
            )

            setBody(
                DelegationRequest(
                    CoveredBy = fødselsnummer,
                    OfferedBy = søknadsskjema.orgnr,
                    RedirectUrl = søknadsskjema.redirectUrl,
                    RequestResources = listOf(
                        RequestResource(
                            ServiceCode = søknadsskjema.serviceCode,
                            ServiceEditionCode = søknadsskjema.serviceEdition,
                        )
                    ),
                )
            )
        }

        return response.body<DelegationRequest?>().let {
            AltinnTilgangssøknad(
                status = it!!.RequestStatus,
                submitUrl = it.links!!.sendRequest!!.href,
            )
        }
    }
}

@Suppress("PropertyName")
@Serializable
data class DelegationRequest(
    val RequestStatus: String? = null,
    val OfferedBy: String? = null,
    val CoveredBy: String? = null,
    val RedirectUrl: String? = null,
    val Created: String? = null,
    val LastChanged: String? = null,
    val KeepSessionAlive: Boolean = true,
    val RequestResources: List<RequestResource>? = null,
    @SerialName("_links") val links: Links? = null
) {

    @Serializable
    data class RequestResource(
        val ServiceCode: String? = null,
        val ServiceEditionCode: Int? = null
    )

    @Serializable
    data class Links(var sendRequest: Link? = null)

    @Serializable
    data class Link(var href: String? = null)
}

@Serializable
data class Søknadsstatus(
    @SerialName("_embedded")
    val embedded: Embedded? = null,
    val continuationtoken: String? = null,
) {
    @Serializable
    data class Embedded(
        val delegationRequests: List<DelegationRequest>? = null
    )
}


@Serializable
data class AltinnTilgangssøknad(
    val orgnr: String? = null,
    val serviceCode: String? = null,
    val serviceEdition: Int? = null,
    val status: String? = null,
    val createdDateTime: String? = null,
    val lastChangedDateTime: String? = null,
    val submitUrl: String? = null
)

@Serializable
data class AltinnTilgangssøknadsskjema(
    val orgnr: String,
    val redirectUrl: String,
    val serviceCode: String,
    val serviceEdition: Int,
)