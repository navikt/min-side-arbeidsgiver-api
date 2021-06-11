package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.dittNavArbeidsgiver.models.NarmesteLedertilgang;
import no.nav.tag.dittNavArbeidsgiver.services.digisyfo.DigisyfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils.ISSUER;
import static no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
public class DigisyfoController {

    private final DigisyfoService digisyfoService;

    @Autowired
    public DigisyfoController(DigisyfoService digisyfoService) {
        this.digisyfoService = digisyfoService;
    }

    @GetMapping(value = "/api/narmesteleder")
    public NarmesteLedertilgang sjekkNarmestelederTilgang() {
        return new NarmesteLedertilgang(
                !digisyfoService.getNarmesteledere().getAnsatte().isEmpty()
        );
    }

}

