package no.nav.arbeidsgiver.min_side.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.NarmesteLedertilgang;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository;
import no.nav.arbeidsgiver.min_side.services.ereg.EregService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.ISSUER;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class DigisyfoController {

    private final NærmestelederRepository nærmestelederRepository;
    private final EregService eregService;
    private final AuthenticatedUserHolder tokenUtils;

    @Autowired
    public DigisyfoController(
            NærmestelederRepository nærmestelederRepository,
            EregService eregService,
            AuthenticatedUserHolder authenticatedUserHolder
    ) {
        this.nærmestelederRepository = nærmestelederRepository;
        this.eregService = eregService;
        this.tokenUtils = authenticatedUserHolder;
    }

    @GetMapping(value = "/api/narmesteleder")
    public NarmesteLedertilgang sjekkNarmestelederTilgang() {
        boolean erNærmesteLeder = !hentVirksomheter().isEmpty();
        return new NarmesteLedertilgang(erNærmesteLeder);
    }

    @GetMapping(value = "/api/narmesteleder/virksomheter")
    public List<Organisasjon> hentVirksomheter() {
        return nærmestelederRepository.virksomheterSomNærmesteLeder(tokenUtils.getFnr()).stream()
                .map(eregService::hentUnderenhet)
                .filter(Objects::nonNull)
                .map(this::tilOrganisasjon)
                .collect(Collectors.toList());
    }

    /**
     * Mapper til samme struktur som bedriftsmenyen forventer.
     */
    private Organisasjon tilOrganisasjon(EregService.Underenhet underenhet) {
        return new Organisasjon(
                underenhet.getNavn(),
                "Business",
                underenhet.getOverordnetEnhet(),
                underenhet.getOrganisasjonsnummer(),
                "BEDR",
                "Active"
        );
    }

}

