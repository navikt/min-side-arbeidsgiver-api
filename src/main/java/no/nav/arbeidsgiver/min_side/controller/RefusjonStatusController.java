package no.nav.arbeidsgiver.min_side.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.exceptions.TilgangskontrollException;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService;
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.ISSUER;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class RefusjonStatusController {

    private static final String TJENESTEKODE = ""; // TODO: wat it is?
    private static final String TJENESTEVERSJON = ""; // TODO: wat it is?

    private final AltinnService altinnService;
    private final RefusjonStatusRepository refusjonStatusRepository;
    private final AuthenticatedUserHolder authenticatedUserHolder;

    @Autowired
    public RefusjonStatusController(
            AltinnService altinnService,
            RefusjonStatusRepository refusjonStatusRepository,
            AuthenticatedUserHolder authenticatedUserHolder
    ) {
        this.altinnService = altinnService;
        this.refusjonStatusRepository = refusjonStatusRepository;
        this.authenticatedUserHolder = authenticatedUserHolder;
    }

    @GetMapping(value = "/api/refusjon_status/{virksomhetsnummer}")
    public RefusjonStatusOversikt statusoversikt(@PathVariable String virksomhetsnummer) {
        checkTilgangTilVirksomhet(virksomhetsnummer);

        return new RefusjonStatusOversikt(refusjonStatusRepository.statusoversikt(virksomhetsnummer));
    }

    private void checkTilgangTilVirksomhet(String virksomhetsnummer) {
        List<Organisasjon> organisasjoner = altinnService
                .hentOrganisasjonerBasertPaRettigheter(authenticatedUserHolder.getFnr(), TJENESTEKODE, TJENESTEVERSJON);

        if (organisasjoner.stream().noneMatch(org -> Objects.equals(org.getOrganizationNumber(), virksomhetsnummer))) {
            throw new TilgangskontrollException(format(
                    "inlogget bruker har ikke tilgang til %s for %s %s",
                    virksomhetsnummer, TJENESTEKODE, TJENESTEVERSJON
            ));
        }
    }

    @AllArgsConstructor
    static class RefusjonStatusOversikt {
        final Map<String, Integer> statusoversikt;

        public Boolean getTilgang() {
            return statusoversikt.values().stream().anyMatch(integer -> integer > 0);
        }
    }

}

