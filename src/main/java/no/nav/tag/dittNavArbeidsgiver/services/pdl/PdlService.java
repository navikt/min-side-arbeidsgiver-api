package no.nav.tag.dittNavArbeidsgiver.services.pdl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.PdlBatchRequest;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.PdlBatchRespons;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.Variables;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRequest;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRespons;
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

    private final GraphQlUtilsBatchSporring graphQlUtilsBatch;
    private final STSClient stsClient;
    private final RestTemplate restTemplate;
    @Value("${pdl.pdlUrl}")
    String pdlUrl;

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

    public PdlBatchRespons getBatchFraPdl(String[] fnrs){
        log.error("MSA-AAREG-PDL: henter navn fra PDL: antall:" + fnrs.length);
        try {
            PdlBatchRequest pdlRequest = new PdlBatchRequest(graphQlUtilsBatch.resourceAsString(), new Variables(fnrs));
            HttpEntity entity = createRequestEntityBatchSporring(pdlRequest);
            return  restTemplate.postForObject(pdlUrl, createRequestEntityBatchSporring(pdlRequest), PdlBatchRespons.class);
        } catch (RestClientException | IOException exception) {
            log.error("MSA-AAREG-PDL: Exception: {} i PDLBATCH" + exception.getMessage());
        }
        return null;
    };
}