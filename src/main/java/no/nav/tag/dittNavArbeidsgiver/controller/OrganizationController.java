package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.models.Organization;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnGW;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
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
        log.debug("Response from altinn" + result.size());
        return ResponseEntity.ok(result);
    }
}
