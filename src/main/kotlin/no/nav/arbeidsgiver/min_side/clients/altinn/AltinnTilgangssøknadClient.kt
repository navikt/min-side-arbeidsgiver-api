package no.nav.arbeidsgiver.min_side.clients.altinn

import no.nav.arbeidsgiver.min_side.clients.altinn.dto.DelegationRequest
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.DelegationRequest.RequestResource
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.Søknadsstatus
import no.nav.arbeidsgiver.min_side.clients.retryInterceptor
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknad
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknadsskjema
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnConfig
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException.BadGateway
import org.springframework.web.util.UriComponentsBuilder
import java.util.function.Consumer
import javax.net.ssl.SSLHandshakeException

@Component
class AltinnTilgangssøknadClient(
    restTemplateBuilder: RestTemplateBuilder,
    altinnConfig: AltinnConfig
) {
    private val log = logger()

    private val restTemplate = restTemplateBuilder
        .additionalInterceptors(
            retryInterceptor(
                maxAttempts = 3,
                backoffPeriod = 250L,
                SSLHandshakeException::class.java
            )
        )
        .build()

    private val delegationRequestApiPath = UriComponentsBuilder
        .fromUriString(altinnConfig.proxyFallbackUrl)
        .path("/ekstern/altinn/api/serviceowner/delegationRequests")
        .build()
        .toUriString()
    private val altinnHeaders = Consumer { httpHeaders: HttpHeaders ->
        httpHeaders.putAll(
            mapOf(
                "accept" to listOf("application/hal+json"),
                "apikey" to listOf(altinnConfig.altinnHeader),
                "x-nav-apikey" to listOf(altinnConfig.APIGwHeader)
            )
        )
    }

    fun hentSøknader(fødselsnummer: String): List<AltinnTilgangssøknad> {
        val resultat = ArrayList<AltinnTilgangssøknad>()
        val filter = String.format("CoveredBy eq '%s'", fødselsnummer)
        var continuationtoken: String? = null
        var shouldContinue = true
        while (shouldContinue) {
            val uri =
                delegationRequestApiPath + "?ForceEIAuthentication&" + if (continuationtoken == null) "\$filter={filter}" else "\$filter={filter}&continuation={continuation}"
            val request = RequestEntity.get(uri, filter, continuationtoken).headers(altinnHeaders).build()
            val response: ResponseEntity<Søknadsstatus?> = try {
                restTemplate.exchange(request, object : ParameterizedTypeReference<Søknadsstatus?>() {})
            } catch (e: BadGateway) {
                log.info("retry pga bad gateway mot altinn {}", e.message)
                restTemplate.exchange(request, object : ParameterizedTypeReference<Søknadsstatus?>() {})
            }
            val body = response.body
            if (body == null) {
                log.warn("Altinn delegation requests: body missing")
                break
            }
            if (body.embedded!!.delegationRequests!!.isEmpty()) {
                shouldContinue = false
            } else {
                continuationtoken = body.continuationtoken
            }
            body.embedded!!.delegationRequests!!
                .map { søknadDTO: DelegationRequest ->
                    val søknad = AltinnTilgangssøknad()
                    søknad.orgnr = søknadDTO.OfferedBy
                    søknad.status = søknadDTO.RequestStatus
                    søknad.createdDateTime = søknadDTO.Created
                    søknad.lastChangedDateTime = søknadDTO.LastChanged
                    søknad.serviceCode = søknadDTO.RequestResources!![0].ServiceCode
                    søknad.serviceEdition = søknadDTO.RequestResources!![0].ServiceEditionCode
                    søknad.submitUrl = søknadDTO.links!!.sendRequest!!.href
                    søknad
                }.toCollection(resultat)
        }
        return resultat
    }

    fun sendSøknad(fødselsnummer: String?, søknadsskjema: AltinnTilgangssøknadsskjema): AltinnTilgangssøknad {
        val requestResource = RequestResource()
        requestResource.ServiceCode = søknadsskjema.serviceCode
        requestResource.ServiceEditionCode = søknadsskjema.serviceEdition
        val delegationRequest = DelegationRequest()
        delegationRequest.CoveredBy = fødselsnummer
        delegationRequest.OfferedBy = søknadsskjema.orgnr
        delegationRequest.RedirectUrl = søknadsskjema.redirectUrl
        delegationRequest.RequestResources = listOf(requestResource)
        val request = RequestEntity
            .post("$delegationRequestApiPath?ForceEIAuthentication")
            .headers(altinnHeaders)
            .body(delegationRequest)
        val response = restTemplate.exchange(request, object : ParameterizedTypeReference<DelegationRequest?>() {})
        val body = response.body
        val svar = AltinnTilgangssøknad()
        svar.status = body!!.RequestStatus
        svar.submitUrl = body.links!!.sendRequest!!.href
        return svar
    }
}