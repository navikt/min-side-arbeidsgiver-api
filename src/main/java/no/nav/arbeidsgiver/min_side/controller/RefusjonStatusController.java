package no.nav.arbeidsgiver.min_side.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService;
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.ISSUER;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;


@ProtectedWithClaims(issuer = ISSUER, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class RefusjonStatusController {

    private static final String TJENESTEKODE = "4936";
    private static final String TJENESTEVERSJON = "1";

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

    @GetMapping(value = "/api/refusjon_status")
    public List<Statusoversikt> statusoversikt() {
        /* Man kan muligens filtrere organisasjoner ytligere med ("BEDR", annet?). */
        var orgnr = altinnService
                .hentOrganisasjonerBasertPaRettigheter(authenticatedUserHolder.getFnr(), TJENESTEKODE, TJENESTEVERSJON)
                .stream()
                .map(Organisasjon::getOrganizationNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return refusjonStatusRepository
                .statusoversikt(orgnr)
                .stream()
                .map(Statusoversikt::from)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused") // DTO
    @Data
    static class Statusoversikt {
        final String virksomhetsnummer;
        final Map<String, Integer> statusoversikt;

        public Boolean getTilgang() {
            return statusoversikt.values().stream().anyMatch(integer -> integer > 0);
        }

        public static Statusoversikt from(RefusjonStatusRepository.Statusoversikt statusoversikt) {
            return new Statusoversikt(
                    statusoversikt.virksomhetsnummer,
                    statusoversikt.statusoversikt
            );
        }
    }
}

