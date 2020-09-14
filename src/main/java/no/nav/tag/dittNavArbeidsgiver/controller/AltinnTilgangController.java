package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.Protected;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.tag.dittNavArbeidsgiver.clients.altinn.AltinnTilgangssøknadClient;
import no.nav.tag.dittNavArbeidsgiver.models.AltinnTilgangssøknad;
import no.nav.tag.dittNavArbeidsgiver.models.AltinnTilgangssøknadsskjema;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@Protected
@Slf4j
@RestController
@RequestMapping("/api/altinnTilgangssoeknad")
public class AltinnTilgangController {
    private final AltinnTilgangssøknadClient altinnClient;
    private final TokenValidationContextHolder requestContextHolder;
    private final Pattern orgnrPattern = Pattern.compile("[0-9]+");

    @Autowired
    public AltinnTilgangController(
            AltinnTilgangssøknadClient altinnClient,
            TokenValidationContextHolder requestContextHolder
    ) {
        this.altinnClient = altinnClient;
        this.requestContextHolder = requestContextHolder;
    }

    @GetMapping()
    public ResponseEntity<List<AltinnTilgangssøknad>> mineSøknaderOmTilgang(@RequestParam String orgnr) {
        if (!orgnrPattern.matcher(orgnr).matches()) {
            log.warn("GET /api/altinnTilgangssoeknad: ugyldig orgnr");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String fødselsnummer = FnrExtractor.extract(requestContextHolder);
        return ResponseEntity.ok(altinnClient.hentSøknader(fødselsnummer, orgnr));
    }

    @PostMapping()
    public ResponseEntity<AltinnTilgangssøknad> sendSøknadOmTilgang(@RequestBody AltinnTilgangssøknadsskjema søknadsskjema) {
        var fødselsnummer= FnrExtractor.extract(requestContextHolder);
        return ResponseEntity.ok(altinnClient.sendSøknad(fødselsnummer, søknadsskjema));
    }
}
