package no.nav.arbeidsgiver.min_side.tilgangssoknad

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.config.retryInterceptor
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.DelegationRequest.RequestResource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

@Suppress("UastIncorrectHttpHeaderInspection")
@Component
class AltinnTilgangssøknadClient(
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${altinn.apiBaseUrl}") altinnApiBaseUrl: String,
    @Value("\${altinn.altinnHeader}") private val altinnApiKey: String,
    private val maskinportenTokenService: MaskinportenTokenService,
) {
    private val log = logger()

    private val restTemplate = restTemplateBuilder
        .rootUri(altinnApiBaseUrl)
        .additionalInterceptors(
            retryInterceptor(
                maxAttempts = 3,
                backoffPeriod = 250L,
                HttpServerErrorException.BadGateway::class.java,
                HttpServerErrorException.ServiceUnavailable::class.java,
                HttpServerErrorException.GatewayTimeout::class.java,
                SocketException::class.java,
                SSLHandshakeException::class.java,
                ResourceAccessException::class.java,
            )
        )
        .build()

    private val delegationRequestApiPath = UriComponentsBuilder
        .fromUriString(altinnApiBaseUrl)
        .path("/api/serviceowner/delegationRequests")
        .build()
        .toUriString()

    fun hentSøknader(fødselsnummer: String): List<AltinnTilgangssøknad> {
        val filter = "CoveredBy eq '$fødselsnummer'"
        var shouldContinue = true
        var continuationtoken: String? = null
        val resultat = ArrayList<AltinnTilgangssøknad>()
        while (shouldContinue) {
            val uri =
                "$delegationRequestApiPath?ForceEIAuthentication&${
                    if (continuationtoken == null) {
                        "\$filter={filter}"
                    } else {
                        "\$filter={filter}&continuation={continuation}"
                    }
                }"

            val body = try {
                restTemplate.exchange<Søknadsstatus?>(
                    RequestEntity.get(uri, filter, continuationtoken)
                        .headers(HttpHeaders().apply {
                            set("accept", "application/hal+json")
                            set("apikey", altinnApiKey)
                            setBearerAuth(maskinportenTokenService.currentAccessToken())
                        }).build()
                ).body
            } catch (e: BadRequest) {
                if (e.message!!.contains("User profile")) { // Altinn returns 400 if user does not exist
                    null
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

            body.embedded.delegationRequests!!.mapTo(resultat) { søknadDTO: DelegationRequest ->
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

    fun sendSøknad(fødselsnummer: String, søknadsskjema: AltinnTilgangssøknadsskjema): AltinnTilgangssøknad {
        val response = restTemplate.exchange<DelegationRequest?>(
            RequestEntity
                .post("$delegationRequestApiPath?ForceEIAuthentication")
                .headers(HttpHeaders().apply {
                    set("accept", "application/hal+json")
                    set("apikey", altinnApiKey)
                    setBearerAuth(maskinportenTokenService.currentAccessToken())
                })
                .body(
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
        )
        return response.body.let {
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