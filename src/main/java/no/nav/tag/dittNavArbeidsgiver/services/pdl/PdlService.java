package no.nav.tag.dittNavArbeidsgiver.services.pdl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.PdlBatchRequest;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.PdlBatchRespons;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.VariablesPdlBatch;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRequest;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRespons;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Variables;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import no.nav.tag.dittNavArbeidsgiver.utils.GraphQlUtils;
import no.nav.tag.dittNavArbeidsgiver.utils.GraphQlUtilsBatchSporring;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlService {

    private final GraphQlUtils graphQlUtils;
    private final GraphQlUtilsBatchSporring graphQlUtilsBatch;
    private final STSClient stsClient;
    private final RestTemplate restTemplate;
    @Value("${pdl.pdlUrl}")
    String pdlUrl;

    @SneakyThrows
    public String hentNavnMedFnr(String fnr){
        Navn result = getFraPdl(fnr);
        String navn = "";
        if(result.fornavn!=null) navn += result.fornavn;
        if(result.mellomNavn!=null) navn += " " +result.mellomNavn;
        if(result.etternavn!=null) navn += " " + result.etternavn;
        return navn;
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

    private HttpEntity<String> createRequestEntityBatchSporring(PdlBatchRequest pdlRequest) {
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
        try {
            PdlRequest pdlRequest = new PdlRequest(graphQlUtils.resourceAsString(), new Variables(fnr));
            log.info("PDL enkel sporring" + pdlRequest);
            PdlRespons respons = restTemplate.postForObject(pdlUrl, createRequestEntity(pdlRequest), PdlRespons.class);
            return lesNavnFraPdlRespons(respons);
        } catch (RestClientException | IOException exception) {
            log.error("MSA-AAREG Exception: {}" , exception.getMessage());
            return lagManglerNavnException();
        }
    }


    public PdlBatchRespons getBatchFraPdl(String[] fnrs){
        String listeMedFnrSomString = arrayTilString(fnrs);
        try {
            PdlBatchRequest pdlRequest = new PdlBatchRequest(graphQlUtilsBatch.resourceAsString(), new VariablesPdlBatch(listeMedFnrSomString));
            log.info("MSA-AAREG: PDLBATCHREQUEST: " +pdlRequest);
            return  restTemplate.postForObject(pdlUrl, createRequestEntityBatchSporring(pdlRequest), PdlBatchRespons.class);
        } catch (RestClientException | IOException exception) {
            log.error("MSA-AAREG Exception: {}" , exception.getMessage());
        }
        return null;
    };

    public String arrayTilString(String [] array) {
        StringBuilder tilString = new StringBuilder("[" + array[0]);
        for (int i = 1; i < array.length; i++) {
            tilString.append(",").append(array[i]);
        }
        return tilString.toString() + "]";
    }

    public PdlBatchRequest getBatchFraPdltest(String [] listeMEdFnr){
        String listeMedFnrSomString = arrayTilString(listeMEdFnr);
        try {
            return new PdlBatchRequest(graphQlUtilsBatch.resourceAsString(), new VariablesPdlBatch(listeMedFnrSomString));
        }
        catch (IOException exception) {
            log.info("FAIL");
        }
        return null;

    };
}

