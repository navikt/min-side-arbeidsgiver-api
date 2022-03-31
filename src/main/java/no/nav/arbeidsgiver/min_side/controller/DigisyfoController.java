package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.NarmesteLedertilgang;
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository;
import no.nav.arbeidsgiver.min_side.utils.TokenUtils;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.ISSUER;
import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class DigisyfoController {

    private final NærmestelederRepository nærmestelederRepository;
    private final TokenUtils tokenUtils;

    @Autowired
    public DigisyfoController(
            NærmestelederRepository nærmestelederRepository,
            TokenUtils tokenUtils
    ) {
        this.nærmestelederRepository = nærmestelederRepository;
        this.tokenUtils = tokenUtils;
    }

    @GetMapping(value = "/api/narmesteleder")
    public NarmesteLedertilgang sjekkNarmestelederTilgang() {
        String fnr = tokenUtils.getFnrForInnloggetBruker();
        boolean erNærmesteLeder = nærmestelederRepository.erNærmesteLederForNoen(fnr);
        return new NarmesteLedertilgang(erNærmesteLeder);
    }

}

