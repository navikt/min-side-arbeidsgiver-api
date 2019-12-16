package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.services.kodeverk.Betydninger;
import no.nav.tag.dittNavArbeidsgiver.services.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

public class YrkesBeskrivelseController {
    private final KodeverkService kodeverkService;

    @Autowired
    public YrkesBeskrivelseController(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    @GetMapping(value = "/api/yrkeskoder")
    public ResponseEntity<Betydninger> hentBeskrivelser() {
        Betydninger response = kodeverkService.hentBetydningerAvYrkeskoder();
        return ResponseEntity.ok(response);

    }
}



