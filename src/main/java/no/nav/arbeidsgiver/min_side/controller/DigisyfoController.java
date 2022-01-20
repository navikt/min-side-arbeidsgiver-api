package no.nav.arbeidsgiver.min_side.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.NarmesteLedertilgang;
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository;
import no.nav.arbeidsgiver.min_side.utils.FnrExtractor;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
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
    private final TokenValidationContextHolder requestContextHolder;
    private final Counter harTilgang;
    private final Counter harIkkeTilgang;

    @Autowired
    public DigisyfoController(
            MeterRegistry meterRegistry,
            NærmestelederRepository nærmestelederRepository,
            TokenValidationContextHolder requestContextHolder
    ) {
        this.nærmestelederRepository = nærmestelederRepository;
        this.requestContextHolder = requestContextHolder;
        // TODO: ta bort disse coutnerne når vi ser at det går bra i prod
        harTilgang = meterRegistry.counter("narmesteleder_tilgang", "hartilgang", "ja");
        harIkkeTilgang = meterRegistry.counter("narmesteleder_tilgang", "hartilgang", "nei");
    }

    @GetMapping(value = "/api/narmesteleder")
    public NarmesteLedertilgang sjekkNarmestelederTilgang() {
        boolean erNærmesteLeder;
        String fnr = FnrExtractor.extract(requestContextHolder);
        erNærmesteLeder = nærmestelederRepository.erNærmesteLederForNoen(fnr);
        (erNærmesteLeder ? harTilgang : harIkkeTilgang).increment();
        return new NarmesteLedertilgang(erNærmesteLeder);
    }

}

