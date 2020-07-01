package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.oidc.api.Protected;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Protected
public class InnloggingsController {
    @RequestMapping(value="/api/innlogget", method = RequestMethod.GET)
    @ResponseBody
    public String erInnlogget(){return "ok";}
}