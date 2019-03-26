package no.nav.tag.dittNavArbeidsgiver.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.utils.AadAccessToken;
import no.nav.tag.dittNavArbeidsgiver.utils.AccesstokenClient;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.api.Protected;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnException;
import no.nav.tag.dittNavArbeidsgiver.utils.AktorClient;
import no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor;
import org.springframework.beans.factory.annotation.Autowired;
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
        @Autowired
        public DigisyfoController (OIDCRequestContextHolder requestContextHolder, AccesstokenClient accesstokenClient, AktorClient aktorClient) {
            this.requestContextHolder = requestContextHolder;
            this.accesstokenClient = accesstokenClient;
            this.aktorClient = aktorClient;
        }


        @GetMapping(value = "/api/narmesteleder")
        public String sjekkNarmestelederTilgang() {
            String fnr = FnrExtractor.extract(requestContextHolder);
            RestTemplate restTemplate =new RestTemplate();
            AadAccessToken adToken = accesstokenClient.hentAccessToken();
            HttpHeaders headers = new HttpHeaders();
            String aktorid = aktorClient.getAktorId(fnr);
            headers.set(OIDCConstants.AUTHORIZATION_HEADER, "Bearer "+adToken.getAccess_token());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = "https://syfoarbeidsgivertilgang.nais.preprod.local/api/"+aktorid;

            try {
                ResponseEntity<String> respons = restTemplate.exchange(url,
                        HttpMethod.GET, entity, String.class);
                return respons.getBody();

            } catch (RestClientException exception) {
                log.error(" Ddigisyfo Exception: ", exception);
                throw new AltinnException("digisyfo", exception);
            }

        }

    }

