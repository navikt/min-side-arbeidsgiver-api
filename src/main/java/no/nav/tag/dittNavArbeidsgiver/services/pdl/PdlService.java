package no.nav.tag.dittNavArbeidsgiver.services.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlPerson;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnException;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PdlService {
    private final RestTemplate restTemplate;
    private final STSClient stsClient;
    @Value("${pdl.pdlUrl}")
    private String pdlUrl;

    public PdlService(RestTemplate restTemplate, STSClient stsClient) {
        this.restTemplate = restTemplate;
        this.stsClient = stsClient;
    }
    public String hentNavnMedFnr(String fnr){

        Navn result = getFraPdl(fnr);
        return result.fornavn +" "+ result.mellomNavn + " "+result.etternavn;

    }
    private HttpEntity<String> createRequestEntity(String fnr) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + stsClient.getToken().getAccess_token());
        headers.set("Tema", "GEN");
        headers.set("Nav-Consumer-Token", stsClient.getToken().getAccess_token());
        headers.set("Content-Type","application/json");
        return new HttpEntity<>(createQuery(fnr),headers);
    }

    private String createQuery(String fnr) {
        return "{\"query\" : \"query{ hentPerson( ident: \\\"" + fnr + "\\\") {navn(historikk: false) {fornavn mellomnavn etternavn} } }\"}";
    }
    private Navn getFraPdl(String fnr){
        try {
            ResponseEntity<PdlPerson> result = restTemplate.exchange(pdlUrl, HttpMethod.POST, createRequestEntity(fnr), PdlPerson.class);
            if (result.getStatusCode()!= HttpStatus.OK){
                String message = "Kall mot pdl feiler med HTTP-" + result.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);
            }
            return result.getBody().data.hentPerson.navn[0];
        } catch (RestClientException exception) {
            log.error("Feil fra PDL med spørring:{} ", pdlUrl);
                    log.error(" Exception: {}" , exception.getMessage());
                    log.error("query til pdl: {}",createRequestEntity(fnr));
            throw new AltinnException("Feil fra PDL", exception);
        }
    }

}

