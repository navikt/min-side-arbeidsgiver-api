package no.nav.arbeidsgiver.min_side.controller;


import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
public class HealthcheckController {
    @GetMapping( "/internal/isAlive")
    public String isAlive() {
        return "ok";
    }

    @GetMapping("/internal/isReady")
    public String isReady() {
        return "ok";
    }
}
