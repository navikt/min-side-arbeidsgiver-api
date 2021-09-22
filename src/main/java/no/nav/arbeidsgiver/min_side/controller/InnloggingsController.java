package no.nav.arbeidsgiver.min_side.controller;

import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.ISSUER;
import static no.nav.arbeidsgiver.min_side.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer=ISSUER, claimMap={REQUIRED_LOGIN_LEVEL})
@RestController
public class InnloggingsController {
    @RequestMapping(value="/api/innlogget", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> erInnlogget(){
        CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS).noStore();
        return ResponseEntity.ok()
            .cacheControl(cacheControl)
            .body("ok");
        }
}