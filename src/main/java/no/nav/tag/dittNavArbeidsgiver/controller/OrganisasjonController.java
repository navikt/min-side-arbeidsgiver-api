package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.Role;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnService;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Protected
@Slf4j
@RestController
public class OrganisasjonController {

    private final AltinnService altinnService;
    private final OIDCRequestContextHolder requestContextHolder;

    @Autowired
    public OrganisasjonController(AltinnService altinnService, OIDCRequestContextHolder requestContextHolder) {
        this.altinnService = altinnService;
        this.requestContextHolder = requestContextHolder;
    }

    @GetMapping(value="/api/organisasjoner")
    public ResponseEntity<List<Organisasjon>> hentOrganisasjoner() {
        String fnr = FnrExtractor.extract(requestContextHolder);
        List <Organisasjon> result = altinnService.hentOrganisasjoner(fnr);
        log.info("antall organisasjoner fra altinn:", result.size());
        return ResponseEntity.ok(result);
    }
    @GetMapping(value ="/api/roller/{orgnr}")
    public ResponseEntity<List<Role>> hentRoller(@PathVariable String orgnr){
        String fnr = FnrExtractor.extract(requestContextHolder);
        List<Role> result = altinnService.hentRoller(fnr, orgnr);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value ="/api/rettigheter-til-skjema")
    public ResponseEntity<List<Organisasjon>> hentRettigheter(@RequestParam String serviceKode, @RequestParam String serviceEdition){
        String fnr = FnrExtractor.extract(requestContextHolder);
        List<Organisasjon> result = altinnService.hentOrganisasjonerBasertPaRettigheter(fnr, serviceKode,serviceEdition);
        return ResponseEntity.ok(result);
    }
}
