package no.nav.tag.dittNavArbeidsgiver.services.pdl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRequest;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRespons;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Variables;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import no.nav.tag.dittNavArbeidsgiver.utils.GraphQlUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlService {

    private final GraphQlUtils graphQlUtils;
    private final STSClient stsClient;
    private final RestTemplate restTemplate;
    @Value("${pdl.pdlUrl}")
    String pdlUrl;

    @SneakyThrows
    @Async
    public CompletableFuture<String> hentNavnMedFnr(String fnr){
        if(fnr.equals("27106124243")){
            log.info("sover tråd");
            Thread.sleep(2000);
            log.info("våkner tråd");
        }
        Navn result = getFraPdl(fnr);
        String navn = "";
        if(result.fornavn!=null) navn += result.fornavn;
        if(result.mellomNavn!=null) navn += " " +result.mellomNavn;
        if(result.etternavn!=null) navn += " " + result.etternavn;
         return CompletableFuture.completedFuture(navn);
    }
    private HttpHeaders createHeaders () {
        String stsToken = stsClient.getToken().getAccess_token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(stsToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tema", "GEN");
        headers.set("Nav-Consumer-Token", "Bearer " + stsToken);
        return headers;
    }
    private HttpEntity<String> createRequestEntity(PdlRequest pdlRequest) {
        return new HttpEntity(pdlRequest,createHeaders());
    }

    private Navn lagManglerNavnException(){
        Navn exceptionNavn = new Navn();
        exceptionNavn.fornavn="Kunne ikke hente navn";
        return exceptionNavn;
    }

    private Navn lesNavnFraPdlRespons(PdlRespons respons){
           try{
            return respons.data.hentPerson.navn[0];
        }catch(NullPointerException | ArrayIndexOutOfBoundsException e){
            log.error("MSA-AAREG nullpointer exception: {} ", e.getMessage());
            if(respons.errors!=null && !respons.errors.isEmpty()){
                log.error("MSA-AAREG pdlerror: " + respons.errors.get(0).message);
            }else {
                log.error("MSA-AAREG nullpointer: helt tom respons fra pdl");
            }
        }
        return lagManglerNavnException();
    }

    private Navn getFraPdl(String fnr){
        String stsToken = stsClient.getToken().getAccess_token();
        try {
            PdlRequest pdlRequest = new PdlRequest(graphQlUtils.resourceAsString(), new Variables(fnr));
            PdlRespons respons = restTemplate.postForObject(pdlUrl, createRequestEntity(pdlRequest), PdlRespons.class);
            return lesNavnFraPdlRespons(respons);
        } catch (RestClientException | IOException exception) {
            log.error("MSA-AAREG Exception: {}" , exception.getMessage());
            return lagManglerNavnException();
        }
    }
}

