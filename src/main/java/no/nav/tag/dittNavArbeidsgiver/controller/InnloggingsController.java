package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

import static no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils.ISSUER;
import static no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer=ISSUER, claimMap={REQUIRED_LOGIN_LEVEL})
@RestController
public class InnloggingsController {
    @RequestMapping(value="/api/innlogget", method = RequestMethod.GET)
    @ResponseBody
    public String erInnlogget(){CacheControl cacheControl = CacheControl.noStore()
            .mustRevalidate();
    return "ok";}
}