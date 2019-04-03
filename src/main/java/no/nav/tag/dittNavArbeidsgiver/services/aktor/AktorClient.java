package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class AktorClient {

    @Value("${aktorregister.aktorUrl}")private String aktorUrl;
    private final RestTemplate restTemplate;
    private final STSClient stsClient;
    private String errorMessage;

    AktorClient(RestTemplate restTemplate,STSClient stsClient){
        this.restTemplate = restTemplate;
        this.stsClient = stsClient;
    }

    public String getAktorId(String fnr){
        String uriString = UriComponentsBuilder.fromHttpUrl(aktorUrl)
                .queryParam("identgruppe","AktoerId")
                .queryParam("gjeldende","true")
                .toUriString();
        HttpEntity<String> entity = getRequestEntity(fnr);
        try {
            ResponseEntity<AktorResponse> response = restTemplate.exchange(uriString, HttpMethod.GET, entity, AktorResponse.class);
            if(checkIfRequestWentWell(response) && checkIfAktorWasFound(fnr,response))
                return (Objects.requireNonNull(response.getBody()).get(fnr).identer.get(0).ident);
            else {
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        }
        catch(HttpClientErrorException e){
            log.error("Feil ved oppslag i aktørtjenesten", e);
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<String> getRequestEntity(String fnr) {
        String appName= "srvditt-nav-arbeid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + stsClient.getToken().getAccess_token());
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", appName);
        headers.set("Nav-Personidenter", fnr);
        return new HttpEntity<>(headers);

    }

    private Boolean checkIfRequestWentWell(ResponseEntity<AktorResponse> response){
        if (response.getStatusCode() != HttpStatus.OK) {
            String message = "Kall mot aktørregister feiler med HTTP-" + response.getStatusCode();
            log.error(message);
            return false;
        }
        return true;
    }

    private Boolean checkIfAktorWasFound(String fnr, ResponseEntity<AktorResponse> response){
        if (response.getBody() != null) {
                if(response.getBody().get(fnr) == null) {
                    errorMessage = "feilmelding på aktør: Fant ikke ident i respons fra aktørregister";
                    return false;
                }
                if (response.getBody().get(fnr).feilmelding != null) {
                    errorMessage = "feilmelding på aktør: " + response.getBody().get(fnr).feilmelding;
                    return false;
                }
            return true;
        }
        errorMessage = "feil i forsøk på å hente aktør, tom mapping fra respons ";
        return false;
    }
}
