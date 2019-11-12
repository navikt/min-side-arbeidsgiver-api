package no.nav.tag.dittNavArbeidsgiver.controller;

import no.finn.unleash.Unleash;
import no.finn.unleash.UnleashContext;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.models.UnleashTilgang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor.extract;


@Protected
@RestController
public class FeatureToggleController {
    private final Unleash unleash;
    private final OIDCRequestContextHolder requestContextHolder;

    @Autowired
    public FeatureToggleController(Unleash unleash, OIDCRequestContextHolder oidcRequestContextHolder) {
        this.unleash = unleash;
        this.requestContextHolder = oidcRequestContextHolder;
    }

    @GetMapping("api/feature")
    public ResponseEntity<UnleashTilgang> feature(
            @RequestParam("feature") String features
    ) {
        String fnr = extract(requestContextHolder);
        UnleashContext context = UnleashContext.builder().userId(fnr).build();
        UnleashTilgang respons = new UnleashTilgang();

        respons.tilgang=unleash.isEnabled(features, context);
        return ResponseEntity.status(HttpStatus.OK).body(respons);
    }
}