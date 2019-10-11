package no.nav.tag.dittNavArbeidsgiver.controller;
import no.nav.security.oidc.api.Protected;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Protected
@Slf4j
@RestController

public class AAregController {

    private final AAregService aAregServiceService;

    @Autowired
    public AAregController(AAregService aAService) {
        this.aAregServiceService = aAService;
    }


    @GetMapping(value = "/api/arbeidsforhold/{orgnr}")
    @ResponseBody
    public ResponseEntity<OversiktOverArbeidsForhold> hentArbeidsforhold(@PathVariable String orgnr) {
        OversiktOverArbeidsForhold result = aAregServiceService.hentArbeidsforhold(orgnr);
        return ResponseEntity.ok(result);
    }
}



