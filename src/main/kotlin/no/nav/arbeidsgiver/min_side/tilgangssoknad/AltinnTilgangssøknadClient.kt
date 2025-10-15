package no.nav.arbeidsgiver.min_side.tilgangssoknad

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.config.Miljø
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import no.nav.arbeidsgiver.min_side.logger
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.DelegationRequest.RequestResource


class AltinnTilgangssøknadClient(
    private val maskinportenTokenService: MaskinportenTokenService,
) {
    private val altinnApiBaseUrl = Miljø.Altinn.baseUrl
    private val altinnApiKey = Miljø.Altinn.altinnHeader

    private val log = logger()

    private val client = defaultHttpClient(configure = {
        expectSuccess = true
    })

    private val delegationRequestApiPath = "$altinnApiBaseUrl/api/serviceowner/delegationRequests"

    suspend fun hentSøknader(fødselsnummer: String): List<AltinnTilgangssøknad> {
        val filter = "CoveredBy eq '$fødselsnummer'".replace(" ", "%20") // URL encode space as %20
        var shouldContinue = true
        var continuationtoken: String? = null
        val resultat = ArrayList<AltinnTilgangssøknad>()
        while (shouldContinue) {
            val uri =
                "$delegationRequestApiPath?${
                    if (continuationtoken == null) {
                        "\$filter=$filter"
                    } else {
                        "\$filter=$filter&continuation=$continuationtoken"
                    }
                }"


            val body = try {
                val response = client.get(uri) {
                    header("accept", "application/hal+json")
                    header("apikey", altinnApiKey)
                    contentType(ContentType.Application.HalJson)
                    bearerAuth(maskinportenTokenService.currentAccessToken())
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

            body.embedded.delegationRequests!!.mapTo(resultat)
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

    suspend fun sendSøknad(fødselsnummer: String, søknadsskjema: AltinnTilgangssøknadsskjema): AltinnTilgangssøknad {
        val response = client.post("$delegationRequestApiPath") {
            header("accept", "application/hal+json")
            header("apikey", altinnApiKey)
            contentType(ContentType.Application.Json)
            bearerAuth(maskinportenTokenService.currentAccessToken())

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

        println("Altinn response: ${response.status}, ${response.bodyAsText()}")

        return response.body<DelegationRequest?>().let {
            AltinnTilgangssøknad(
                status = it!!.RequestStatus,
                submitUrl = it.links!!.sendRequest!!.href,
            )
        }
    }
}

@Suppress("PropertyName")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DelegationRequest(
    val RequestStatus: String? = null,
    val OfferedBy: String? = null,
    val CoveredBy: String? = null,
    val RedirectUrl: String? = null,
    val Created: String? = null,
    val LastChanged: String? = null,
    val KeepSessionAlive: Boolean = true,
    val RequestResources: List<RequestResource>? = null,
    @field:JsonProperty("_links") val links: Links? = null
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RequestResource(
        val ServiceCode: String? = null,
        val ServiceEditionCode: Int? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Links(var sendRequest: Link? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Link(var href: String? = null)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Søknadsstatus(
    @field:JsonProperty("_embedded")
    val embedded: Embedded? = null,
    val continuationtoken: String? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Embedded(
        val delegationRequests: List<DelegationRequest>? = null
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AltinnTilgangssøknad(
    val orgnr: String? = null,
    val serviceCode: String? = null,
    val serviceEdition: Int? = null,
    val status: String? = null,
    val createdDateTime: String? = null,
    val lastChangedDateTime: String? = null,
    val submitUrl: String? = null
)

data class AltinnTilgangssøknadsskjema(
    val orgnr: String,
    val redirectUrl: String,
    val serviceCode: String,
    val serviceEdition: Int,
)