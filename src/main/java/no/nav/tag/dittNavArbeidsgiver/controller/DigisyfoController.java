package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.dittNavArbeidsgiver.models.DigisyfoNarmesteLederRespons;
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
        if (narmesteledere == null) {
            log.error("fikk null response fra digisyfo.narmesteledere");
            return new NarmesteLedertilgang(false);
        }
        if (narmesteledere.getAnsatte() == null) {
            log.error("fikk null response fra digisyfo.narmesteledere.ansatte");
            return new NarmesteLedertilgang(false);
        }
        return new NarmesteLedertilgang(
                !narmesteledere.getAnsatte().isEmpty()
        );
    }

}

