package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Protected
@Slf4j
@RestController
public class DigisyfoController {


        private final OIDCRequestContextHolder requestContextHolder;

        @Autowired
        public DigisyfoController (OIDCRequestContextHolder requestContextHolder) {
            this.requestContextHolder = requestContextHolder;
        }

        @GetMapping(value = "/api/narmesteleder")
        public String sjekkNarmestelederTilgang() {
            log.debug("===========Sjekknærmesteledertilgang===========");
            for (String issuer : requestContextHolder.getOIDCValidationContext().getIssuers()){
                log.debug("issuer: " + issuer);
                log.debug("issuer: " + requestContextHolder.getOIDCValidationContext().getClaims(issuer));
            }

            return "ok";
        }

    }

