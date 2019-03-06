package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrganizationController {

    private final AltinnGW altinnGW;

    @Autowired
    public OrganizationController(AltinnGW altinnGW) {
        this.altinnGW = altinnGW;
    }

    @RequestMapping(value="/api/organisasjoner", method = RequestMethod.GET)
    private ResponseEntity<List<Organization>> getOrganizations(){
        List <Organization> result = altinnGW.getOrganizations("1");
        return ResponseEntity.ok(result);

    }
}
