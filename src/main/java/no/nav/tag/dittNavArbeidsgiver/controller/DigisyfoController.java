package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.services.digisyfo.DigisyfoService;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
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
        @Value("${digisyfo.digisyfoUrl}") private String digisyfoUrl;

        public DigisyfoController (OIDCRequestContextHolder requestContextHolder, DigisyfoService digisyfoService) {
            this.requestContextHolder = requestContextHolder;
            this.digisyfoService = digisyfoService;
        }

        @GetMapping(value = "/api/narmesteleder")
        public String sjekkNarmestelederTilgang() {
            String fnr = FnrExtractor.extract(requestContextHolder);
            return digisyfoService.getNarmesteledere(fnr);
        }

    @GetMapping(value = "/api/sykemeldinger")
    public String hentAntallSykemeldinger (@CookieValue("nav-esso") String navesso ) {
        digisyfoService.hentSykemeldingerFraSyfo(navesso);
        return null;
    }




}

