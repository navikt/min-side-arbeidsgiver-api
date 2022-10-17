package no.nav.arbeidsgiver.min_side.controller;

import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.LOGINSERVICE;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer= LOGINSERVICE, claimMap={REQUIRED_LOGIN_LEVEL})
@RestController
public class InnloggingsController {
    @GetMapping("/api/innlogget")
    public ResponseEntity<String> erInnlogget(){
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body("ok");
        }
}