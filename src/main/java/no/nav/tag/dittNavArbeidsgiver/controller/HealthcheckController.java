package no.nav.tag.dittNavArbeidsgiver.controller;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthcheckController {
    @RequestMapping(value="/internal/isAlive", method = RequestMethod.GET)
    @ResponseBody
    public String isAlive(){return "ok";}

    @RequestMapping(value = "/internal/isReady", method = RequestMethod.GET)
    @ResponseBody
    public String isReady() {
        return "ok";
    }
}
