package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;

import no.finn.unleash.Unleash;
import no.nav.tag.dittNavArbeidsgiver.services.unleash.DNAUnleashConfig;
import no.nav.tag.dittNavArbeidsgiver.services.digisyfo.DigisyfoService;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Protected
@Slf4j
@RestController
public class DigisyfoController {

    private final OIDCRequestContextHolder requestContextHolder;
    private final DigisyfoService digisyfoService;
    private final Unleash unleash;
    @Value("${digisyfo.digisyfoUrl}")
    private String digisyfoUrl;

    @Autowired
    public DigisyfoController(OIDCRequestContextHolder requestContextHolder, DigisyfoService digisyfoService, Unleash unleash) {
        this.requestContextHolder = requestContextHolder;
        this.digisyfoService = digisyfoService;
        this.unleash = unleash;

    }

    @GetMapping(value = "/api/narmesteleder")
    public String sjekkNarmestelederTilgang() {
        String fnr = FnrExtractor.extract(requestContextHolder);
        return digisyfoService.getNarmesteledere(fnr);
    }

    @GetMapping(value = "/api/sykemeldinger")
    public String hentAntallSykemeldinger(@CookieValue("nav-esso") String navesso) {
        if(unleash.isEnabled("dna.digisyfo.hentSykemeldinger")) {
            return digisyfoService.hentSykemeldingerFraSyfo(navesso);
        }else{
            return"[]";
        }
    }
    @GetMapping(value = "/api/syfooppgaver")
    public String hentSyfoOppgaver(@CookieValue("nav-esso") String navesso) {
        if(unleash.isEnabled("dna.digisyfo.hentSyfoOppgaver")) {
            return digisyfoService.hentSyfoOppgaver(navesso);
        }else {
        return "[]";
        }

    }

}

