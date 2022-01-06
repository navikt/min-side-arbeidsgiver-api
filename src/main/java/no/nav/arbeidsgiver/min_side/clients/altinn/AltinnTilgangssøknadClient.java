package no.nav.arbeidsgiver.min_side.clients.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.DelegationRequest;
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.Søknadsstatus;
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknad;
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknadsskjema;
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AltinnTilgangssøknadClient {
    private final RestTemplate restTemplate;
    private final HttpHeaders altinnHeaders;
    private final Supplier<UriComponentsBuilder> altinnUriBuilder;
    private final URI altinnUri;
    private final ParameterizedTypeReference<Søknadsstatus> søknadsstatusType;
    private final ParameterizedTypeReference<DelegationRequest> delegationRequestType;

    @Autowired
    public AltinnTilgangssøknadClient(RestTemplate restTemplate, AltinnConfig altinnConfig) {
        this.restTemplate = restTemplate;

        var altinnUriBuilder = UriComponentsBuilder.fromUriString(
                altinnConfig.getProxyFallbackUrl()
                        + "/ekstern/altinn/api/serviceowner/delegationRequests?ForceEIAuthentication"
        );

        this.altinnUriBuilder = altinnUriBuilder::cloneBuilder;


        this.altinnUri = altinnUriBuilder.build().toUri();
        log.info("altinnUri: {}", altinnUri);

        this.altinnHeaders = new HttpHeaders();
        this.altinnHeaders.add("accept", "application/hal+json");
        this.altinnHeaders.add("apikey", altinnConfig.getAltinnHeader());
        this.altinnHeaders.add("x-nav-apikey", altinnConfig.getAPIGwHeader());

        this.søknadsstatusType = new ParameterizedTypeReference<>() {
        };

        this.delegationRequestType = new ParameterizedTypeReference<>() {
        };
    }

    public List<AltinnTilgangssøknad> hentSøknader(String fødselsnummer) {
        var resultat = new ArrayList<AltinnTilgangssøknad>();
        var filter = String.format("CoveredBy eq '%s'", fødselsnummer);
        String continuationtoken = null;
        boolean shouldContinue = true;
        while (shouldContinue) {
            var uri = altinnUriBuilder.get()
                    .query(continuationtoken == null ? "$filter={}" : "$filter={}&continuation={}")
                    .build()
                    .toUriString();
            var request = RequestEntity.get(uri, filter, continuationtoken).headers(altinnHeaders).build();
            ResponseEntity<Søknadsstatus> response;
            try {
                response = restTemplate.exchange(request, søknadsstatusType);
            } catch (HttpServerErrorException.BadGateway e) {
                log.warn("retry pga bad gateway mot altinn {}", e.getMessage());
                response = restTemplate.exchange(request, søknadsstatusType);
            }

            var body = response.getBody();
            if (body == null) {
                log.warn("Altinn delegation requests: body missing");
                break;
            }

            if (body.embedded.delegationRequests.isEmpty()) {
                shouldContinue = false;
            } else {
                continuationtoken = body.continuationtoken;
            }

            body.embedded
                    .delegationRequests
                    .stream()
                    .map(søknadDTO -> {
                        var søknad = new AltinnTilgangssøknad();
                        søknad.setOrgnr(søknadDTO.OfferedBy);
                        søknad.setStatus(søknadDTO.RequestStatus);
                        søknad.setCreatedDateTime(søknadDTO.Created);
                        søknad.setLastChangedDateTime(søknadDTO.LastChanged);
                        søknad.setServiceCode(søknadDTO.RequestResources.get(0).ServiceCode);
                        søknad.setServiceEdition(søknadDTO.RequestResources.get(0).ServiceEditionCode);
                        søknad.setSubmitUrl(søknadDTO.links.sendRequest.href);
                        return søknad;
                    })
                    .collect(Collectors.toCollection(() -> resultat));
        }

        return resultat;
    }

    public AltinnTilgangssøknad sendSøknad(String fødselsnummer, AltinnTilgangssøknadsskjema søknadsskjema) {
        var requestResource = new DelegationRequest.RequestResource();
        requestResource.ServiceCode = søknadsskjema.serviceCode;
        requestResource.ServiceEditionCode = søknadsskjema.serviceEdition;

        var delegationRequest = new DelegationRequest();
        delegationRequest.CoveredBy = fødselsnummer;
        delegationRequest.OfferedBy = søknadsskjema.orgnr;
        delegationRequest.RedirectUrl = søknadsskjema.redirectUrl;
        delegationRequest.RequestResources = List.of(requestResource);

        var request = RequestEntity
                .post(altinnUri)
                .headers(altinnHeaders)
                .body(delegationRequest);

        var response = restTemplate.exchange(request, delegationRequestType);
        var body = response.getBody();

        var svar = new AltinnTilgangssøknad();
        svar.setStatus(body.RequestStatus);
        svar.setSubmitUrl(body.links.sendRequest.href);
        return svar;
    }
}
