package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.LOGINSERVICE;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer= LOGINSERVICE, claimMap={REQUIRED_LOGIN_LEVEL})
@Slf4j
@RestController
public class OrganisasjonController {

    private final AltinnService altinnService;
    private final AuthenticatedUserHolder authenticatedUserHolder;

    @Autowired
    public OrganisasjonController(
            AltinnService altinnService,
            AuthenticatedUserHolder authenticatedUserHolder
    ) {
        this.altinnService = altinnService;
        this.authenticatedUserHolder = authenticatedUserHolder;
    }

    @GetMapping("/api/organisasjoner")
    public List<Organisasjon> hentOrganisasjoner() {
        return altinnService.hentOrganisasjoner(authenticatedUserHolder.getFnr());
    }

    @GetMapping("/api/rettigheter-til-skjema")
    public List<Organisasjon> hentRettigheter(
            @RequestParam String serviceKode,
            @RequestParam String serviceEdition
    ){
        return altinnService.hentOrganisasjonerBasertPaRettigheter(
                authenticatedUserHolder.getFnr(),
                serviceKode,
                serviceEdition
        );
    }
}