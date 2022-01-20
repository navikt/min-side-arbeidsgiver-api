package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.NarmesteLedertilgang;
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository;
import no.nav.arbeidsgiver.min_side.services.digisyfo.deprecated.DigisyfoService;
import no.nav.arbeidsgiver.min_side.utils.FnrExtractor;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.ISSUER;
import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class DigisyfoController {

    private final DigisyfoService digisyfoService;
    private final NærmestelederRepository nærmestelederRepository;
    private final TokenValidationContextHolder requestContextHolder;
    private final boolean erProd;

    @Autowired
    public DigisyfoController(
            Environment environment,
            DigisyfoService digisyfoService,
            NærmestelederRepository nærmestelederRepository,
            TokenValidationContextHolder requestContextHolder
    ) {
        this.digisyfoService = digisyfoService;
        this.nærmestelederRepository = nærmestelederRepository;
        this.requestContextHolder = requestContextHolder;
        erProd = Arrays.asList(environment.getActiveProfiles()).contains("prod-gcp");
    }

    @GetMapping(value = "/api/narmesteleder")
    public NarmesteLedertilgang sjekkNarmestelederTilgang() {
        boolean erNærmesteLeder;
        if (erProd) {
            // TODO: midlertidig kjører vi parallelt i prod slik at vi får tid til å spise 1.2M kafka meldinger
            erNærmesteLeder = digisyfoService.erNærmesteLederForNoen();
        } else {
            String fnr = FnrExtractor.extract(requestContextHolder);
            erNærmesteLeder = nærmestelederRepository.erNærmesteLederForNoen(fnr);
        }

        return new NarmesteLedertilgang(erNærmesteLeder);
    }

}

