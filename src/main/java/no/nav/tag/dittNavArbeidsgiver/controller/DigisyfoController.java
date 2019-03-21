package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
            RestTemplate restTemplate =new RestTemplate();
            log.info("===========Sjekknærmesteledertilgang===========");
            for (String issuer : requestContextHolder.getOIDCValidationContext().getIssuers()){
                log.info("issuer: " + issuer);
                log.info("issuer: " + requestContextHolder.getOIDCValidationContext().getClaims(issuer));
            }
            HttpHeaders headers = new HttpHeaders();
            headers.set("Bearer ", requestContextHolder.getOIDCValidationContext().getToken("selvbetjening").getIdToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = "https://syfoarbeidsgivertilgang.nais.preprod.local/api/06025800174";

            try {
                ResponseEntity<String> respons = restTemplate.exchange(url,
                        HttpMethod.GET, entity, String.class);
                return respons.getBody();

            } catch (RestClientException exception) {
                log.error(" Ddigisyfo Exception: ", exception);
                throw new AltinnException("Feil fra Altinn", exception);
            }
            // rest kall til syfo med header Bearer blabla  context.getToken("selvbetjening").getIdToken()

            // restteplate legg til headers med "Bearer ..."

           // return "ok";
        }

    }

