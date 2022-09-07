package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.arbeidsgiver.min_side.clients.altinn.AltinnTilgangssøknadClient;
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknad;
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknadsskjema;
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.ISSUER;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer=ISSUER, claimMap={REQUIRED_LOGIN_LEVEL})
@Slf4j
@RestController
@RequestMapping("/api/altinn-tilgangssoknad")
public class AltinnTilgangController {
    private static final Set<Pair<String, String>> våreTjenester = Set.of(
            Pair.of("3403", "2"),
            Pair.of("4936",  "1"),
            Pair.of("5078",  "1"),
            Pair.of("5159", "1"),
            Pair.of("5212",  "1"),
            Pair.of("5216",  "1"),
            Pair.of("5278", "1"),
            Pair.of("5332", "1"),
            Pair.of("5332", "2"),
            Pair.of("5384",  "1"),
            Pair.of("5441",  "1"),
            Pair.of("5516", "1"),
            Pair.of("5516", "2"),
            Pair.of("5516", "3"),
            Pair.of("5516", "4"),
            Pair.of("5516", "5")
    );

    private final AltinnTilgangssøknadClient altinnTilgangssøknadClient;
    private final AltinnService altinnService;
    private final AuthenticatedUserHolder tokenUtils;

    @Autowired
    public AltinnTilgangController(
            AltinnTilgangssøknadClient altinnTilgangssøknadClient,
            AltinnService altinnService,
            AuthenticatedUserHolder tokenUtils
    ) {
        this.altinnTilgangssøknadClient = altinnTilgangssøknadClient;
        this.altinnService = altinnService;
        this.tokenUtils = tokenUtils;
    }

    @GetMapping
    public List<AltinnTilgangssøknad> mineSøknaderOmTilgang() {
        String fødselsnummer = tokenUtils.getFnr();
        return altinnTilgangssøknadClient.hentSøknader(fødselsnummer);
    }

    @PostMapping()
    public ResponseEntity<AltinnTilgangssøknad> sendSøknadOmTilgang(@RequestBody AltinnTilgangssøknadsskjema søknadsskjema) {
        var fødselsnummer= tokenUtils.getFnr();

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
