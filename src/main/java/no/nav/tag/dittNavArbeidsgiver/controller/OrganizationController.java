package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OrganizationController {

    @RequestMapping(value="/api/organisasjoner", method = RequestMethod.GET)
    private ResponseEntity<List<Organization>> getOrganizations(){
       List <Organization> result = new ArrayList<>();
       Organization a =  new Organization();
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
       result.add(b);
        return ResponseEntity.ok(result);

    }
}
