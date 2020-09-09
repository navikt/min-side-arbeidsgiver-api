package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.token.support.core.api.Protected;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
public class InnloggingsController {
    @RequestMapping(value="/api/innlogget", method = RequestMethod.GET)
    @ResponseBody
    public String erInnlogget(){return "ok";}
}