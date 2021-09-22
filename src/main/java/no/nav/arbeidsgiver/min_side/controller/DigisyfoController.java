package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.arbeidsgiver.min_side.models.DigisyfoNarmesteLederRespons;
import no.nav.arbeidsgiver.min_side.models.NarmesteLedertilgang;
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.ISSUER;
import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class DigisyfoController {

    private final DigisyfoService digisyfoService;

    @Autowired
    public DigisyfoController(DigisyfoService digisyfoService) {
        this.digisyfoService = digisyfoService;
    }

    @GetMapping(value = "/api/narmesteleder")
    public NarmesteLedertilgang sjekkNarmestelederTilgang() {
        DigisyfoNarmesteLederRespons narmesteledere = digisyfoService.getNarmesteledere();
        return new NarmesteLedertilgang(
                !narmesteledere.getAnsatte().isEmpty()
        );
    }

}

