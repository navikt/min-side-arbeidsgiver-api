package no.nav.tag.dittNavArbeidsgiver.controller;

import no.finn.unleash.Unleash;
import no.nav.security.oidc.api.Unprotected;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
public class TestController {
    private final Unleash unleash;

    public TestController(Unleash unleash) {
        this.unleash = unleash;
    }

    @GetMapping("/enabled")
    public boolean enabled() {
        return unleash.isEnabled("arbeidsgiver.dna.test-enabled");
    }
}
