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
        AltinnGW gw = new AltinnGW();
        List <Organization> result = gw.getOrganizations("1");
        return ResponseEntity.ok(result);

    }
}
