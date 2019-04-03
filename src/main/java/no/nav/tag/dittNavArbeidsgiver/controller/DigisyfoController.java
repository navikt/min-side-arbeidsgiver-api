package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.utils.AadAccessToken;
import no.nav.tag.dittNavArbeidsgiver.utils.AccesstokenClient;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnException;
import no.nav.tag.dittNavArbeidsgiver.services.aktor.AktorClient;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Protected
@Slf4j
@RestController
public class DigisyfoController {

        private final OIDCRequestContextHolder requestContextHolder;
        private final AccesstokenClient accesstokenClient;
        private final AktorClient aktorClient;
        private final RestTemplate restTemplate;
        @Value("${digisyfo.digisyfoUrl}") private String digisyfoUrl;

        public DigisyfoController (OIDCRequestContextHolder requestContextHolder, AccesstokenClient accesstokenClient, AktorClient aktorClient, RestTemplate restTemplate) {
            this.requestContextHolder = requestContextHolder;
            this.accesstokenClient = accesstokenClient;
            this.aktorClient = aktorClient;
            this.restTemplate = restTemplate;
        }

        @GetMapping(value = "/api/narmesteleder")
        public String sjekkNarmestelederTilgang() {
            String fnr = FnrExtractor.extract(requestContextHolder);
            HttpHeaders headers = new HttpHeaders();
            /*
            TODO: lagre accestoken fra AD (med en session?) så man slipper å spørre AD hver gang man skal sjekke nærmeste leder tilgang
             */
            headers.set(OIDCConstants.AUTHORIZATION_HEADER, "Bearer " + accesstokenClient.hentAccessToken().getAccess_token());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = digisyfoUrl + aktorClient.getAktorId(fnr);
            try {
                ResponseEntity<String> respons = restTemplate.exchange(url,
                        HttpMethod.GET, entity, String.class);
                return respons.getBody();
            } catch (RestClientException exception) {
                log.error(" Digisyfo Exception: ", exception);
                throw new AltinnException("digisyfo", exception);
            }
        }

    }

