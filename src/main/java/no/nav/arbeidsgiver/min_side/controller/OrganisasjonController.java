package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.utils.FnrExtractor;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.ISSUER;
import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer=ISSUER, claimMap={REQUIRED_LOGIN_LEVEL})
@Slf4j
@RestController
public class OrganisasjonController {

    private final AltinnService altinnService;
    private final TokenValidationContextHolder requestContextHolder;

    @Autowired
    public OrganisasjonController(AltinnService altinnService, TokenValidationContextHolder requestContextHolder) {
        this.altinnService = altinnService;
        this.requestContextHolder = requestContextHolder;
    }

    @GetMapping(value="/api/organisasjoner")
    public ResponseEntity<List<Organisasjon>> hentOrganisasjoner() {
        String fnr = FnrExtractor.extract(requestContextHolder);
        List <Organisasjon> result = altinnService.hentOrganisasjoner(fnr);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value ="/api/rettigheter-til-skjema")
    public ResponseEntity<List<Organisasjon>> hentRettigheter(@RequestParam String serviceKode, @RequestParam String serviceEdition){
        String fnr = FnrExtractor.extract(requestContextHolder);
        List<Organisasjon> result = altinnService.hentOrganisasjonerBasertPaRettigheter(fnr, serviceKode,serviceEdition);
        return ResponseEntity.ok(result);
    }
}