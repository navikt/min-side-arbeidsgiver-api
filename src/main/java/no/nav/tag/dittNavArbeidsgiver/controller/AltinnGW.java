package no.nav.tag.dittNavArbeidsgiver.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nav.tag.dittNavArbeidsgiver.LoggingRequestInterceptor;
import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class AltinnGW {

    @Value("${ALTINN_HEADER}") String altinnHeader;
    @Value("${APIGW_HEADER}") String APIGwHeader;

    public List<Organization> getOrganizations(String pnr){
        System.out.println("AltinnGW get orgs");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X_NAV_APIKEY", APIGwHeader);
        headers.set("APIKEY", altinnHeader);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        List<Organization> result = new ArrayList<>();

        ResponseEntity <List<Organization>> response = null;

        RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingRequestInterceptor());
        restTemplate.setInterceptors(interceptors);
        response = restTemplate.exchange("https://api-gw-q1.adeo.no/ekstern/altinn/api/serviceowner/reportees/?subject=14044500761&ForceEIAuthentication",
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<Organization>>() {
                });
        result = response.getBody();

        /*Organization a =  new Organization();
        a.setNavn("BIRI OG TORPO REGNSKAP");
        a.setOrgNo("910437127");
        a.setStatus("Active");
        a.setType("Enterprise");
        Organization b =  new Organization();
        b.setNavn( "EIDSNES OG AUSTRE ÅMØY");
        b.setOrgNo("910521551");
        b.setStatus("Active");
        b.setType("Business");
        result.add(a);
        result.add(b);*/
        return result;
    }

}
