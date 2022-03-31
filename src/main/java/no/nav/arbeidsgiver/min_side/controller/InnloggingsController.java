package no.nav.arbeidsgiver.min_side.controller;

import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.ISSUER;
import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer=ISSUER, claimMap={REQUIRED_LOGIN_LEVEL})
@RestController
public class InnloggingsController {
    @RequestMapping(value="/api/innlogget", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> erInnlogget(){
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body("ok");
        }
}