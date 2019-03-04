package no.nav.tag.dittNavArbeidsgiver.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class AltinnGW {

    @Value("${ALTINN_HEADER}") String altinnHeader;
    @Value("${APIGW_HEADER}") String APIGwHeader;

    public List<Organization> getOrganizations(String pnr){
        System.out.println("AltinnGW get orgs");
        List<Organization> result = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity <List<Organization>> response = restTemplate.exchange("https://api-gw-qi.adeo.no/ekstern/api/serviceowner/reportees/?subject=140445000761&ForceEIAuthentication",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Organization>>() {
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
