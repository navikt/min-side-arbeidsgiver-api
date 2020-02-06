package no.nav.tag.dittNavArbeidsgiver.services.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlPerson;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnException;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STStoken;
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
    String pdlUrl;

    public PdlService(RestTemplate restTemplate, STSClient stsClient) {
        this.restTemplate = restTemplate;
        this.stsClient = stsClient;
    }

    public String hentNavnMedFnr(String fnr){
        log.info(" hentNavnMedFnr ");
        Navn result = getFraPdl(fnr);
        log.info(" hentNavnMedFnr fikk navn: " + result);
        String navn = "";
        if(result.fornavn!=null) navn += result.fornavn;
        if(result.mellomNavn!=null) navn += " " +result.mellomNavn;
        if(result.etternavn!=null) navn += " " + result.etternavn;
        return navn;
    }

    private HttpEntity<String> createRequestEntity(String fnr) {
        HttpHeaders headers = new HttpHeaders();
        String ststoken=stsClient.getToken().getAccess_token();
        headers.set("Authorization", "Bearer " + ststoken);
        headers.set("Tema", "GEN");
        headers.set("Nav-Consumer-Token","Bearer " + ststoken);
        headers.set("Content-Type","application/json");
        return new HttpEntity<>(createQuery(fnr),headers);
    }

    private String createQuery(String fnr) {
        return "{\"query\" : \"query{ hentPerson( ident: \\\"" + fnr + "\\\") {navn(historikk: false) {fornavn mellomnavn etternavn} } }\"}";
    }

    private Navn lagManglerNavnException(){
        Navn exceptionNavn = new Navn();
        exceptionNavn.fornavn="Kunne ikke hente navn";
        return exceptionNavn;
    }

    private Navn lesNavnFraPdlRespons(ResponseEntity<PdlPerson> respons){
        log.info(" lesNavnFraPdlRespons respons: " + respons);
        try{
            return respons.getBody().data.hentPerson.navn[0];
        }catch(NullPointerException | ArrayIndexOutOfBoundsException e){
            log.error("nullpointer exception: {} ", e.getMessage());
            return lagManglerNavnException();
        }
    }

    private Navn getFraPdl(String fnr){
        log.info("getFraPdl ");
        try {
            ResponseEntity<PdlPerson> respons = restTemplate.exchange(pdlUrl, HttpMethod.POST, createRequestEntity(fnr), PdlPerson.class);
            if (respons.getStatusCode() != HttpStatus.OK){
                String message = "Kall mot pdl feiler med HTTP-" + respons.getStatusCode();
                log.error(message);
                return lagManglerNavnException();
            }
            log.trace("result get body:{} ",respons.getBody());
            return lesNavnFraPdlRespons(respons);
        } catch (RestClientException exception) {
            log.error(" Exception: {}" , exception.getMessage());
            return lagManglerNavnException();
        }
    }
}

