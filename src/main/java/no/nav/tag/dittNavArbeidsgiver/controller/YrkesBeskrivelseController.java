package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.oidc.api.Unprotected;
import no.nav.tag.dittNavArbeidsgiver.models.Yrkeskoderespons.Yrkeskoderespons;
import no.nav.tag.dittNavArbeidsgiver.services.yrkeskode.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
public class YrkesBeskrivelseController {
    private final KodeverkService kodeverkService;

    @Autowired
    public YrkesBeskrivelseController(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    @GetMapping(value = "/api/yrkeskoder")
    public ResponseEntity<Yrkeskoderespons> hentBeskrivelser() {
        Yrkeskoderespons response = kodeverkService.hentBetydningerAvYrkeskoder();
        return ResponseEntity.ok(response);

    }
}



