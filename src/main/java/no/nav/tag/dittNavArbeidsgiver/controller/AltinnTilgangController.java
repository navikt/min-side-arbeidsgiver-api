package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Pair;
import no.nav.security.token.support.core.api.Protected;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.tag.dittNavArbeidsgiver.clients.altinn.AltinnTilgangssøknadClient;
import no.nav.tag.dittNavArbeidsgiver.models.AltinnTilgangssøknad;
import no.nav.tag.dittNavArbeidsgiver.models.AltinnTilgangssøknadsskjema;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnService;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Protected
@Slf4j
@RestController
@RequestMapping("/api/altinn-tilgangssoknad")
public class AltinnTilgangController {
    private static final Set<Pair<String, String>> våreTjenester = Set.of(
            Pair.of("5216",  "1"),
            Pair.of("5212",  "1"),
            Pair.of("5384",  "1"),
            Pair.of("5159", "1"),
            Pair.of("4936",  "1"),
            Pair.of("5332", "2"),
            Pair.of("5332", "1"),
            Pair.of("5441",  "1"),
            Pair.of("5516", "1"),
            Pair.of("5516", "2"),
            Pair.of("3403", "2"),
            Pair.of("5078",  "1"),
            Pair.of("5278", "1")
    );

    private final AltinnTilgangssøknadClient altinnTilgangssøknadClient;
    private final AltinnService altinnService;
    private final TokenValidationContextHolder requestContextHolder;

    @Autowired
    public AltinnTilgangController(
            AltinnTilgangssøknadClient altinnTilgangssøknadClient,
            AltinnService altinnService,
            TokenValidationContextHolder requestContextHolder
    ) {
        this.altinnTilgangssøknadClient = altinnTilgangssøknadClient;
        this.altinnService = altinnService;
        this.requestContextHolder = requestContextHolder;
    }

    @GetMapping()
    public ResponseEntity<List<AltinnTilgangssøknad>> mineSøknaderOmTilgang() {
        String fødselsnummer = FnrExtractor.extract(requestContextHolder);
        return ResponseEntity.ok(altinnTilgangssøknadClient.hentSøknader(fødselsnummer));
    }

    @PostMapping()
    public ResponseEntity<AltinnTilgangssøknad> sendSøknadOmTilgang(@RequestBody AltinnTilgangssøknadsskjema søknadsskjema) {
        var fødselsnummer= FnrExtractor.extract(requestContextHolder);

        var brukerErIOrg = altinnService.hentOrganisasjoner(fødselsnummer)
                .stream()
                .anyMatch(org -> org.getOrganizationNumber().equals(søknadsskjema.orgnr));

        if (!brukerErIOrg) {
            log.warn("Bruker forsøker å be om tilgang til org de ikke er med i.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Pair<String, String> tjeneste = Pair.of(søknadsskjema.serviceCode, søknadsskjema.serviceEdition.toString());

        if (!våreTjenester.contains(tjeneste)) {
            log.warn("Bruker forsøker å be om tilgang til tjeneste ({}, {})) vi ikke støtter.", søknadsskjema.serviceCode, søknadsskjema.serviceEdition);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok(altinnTilgangssøknadClient.sendSøknad(fødselsnummer, søknadsskjema));
    }
}
