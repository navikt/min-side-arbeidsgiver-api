package no.nav.arbeidsgiver.min_side.services.digisyfo.deprecated;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.stream.Stream;

import static no.nav.security.token.support.core.JwtTokenConstants.AUTHORIZATION_HEADER;

@Slf4j
@Service
@Setter
public class DigisyfoService {

    private final RestTemplate restTemplate;
    private final TokenUtils tokenUtils;
    private final String syfoNarmesteLederUrl;

    @Autowired
    public DigisyfoService(
            RestTemplate restTemplate,
            TokenUtils tokenUtils
    ) {
        this.restTemplate = restTemplate;
        this.tokenUtils = tokenUtils;
        this.syfoNarmesteLederUrl = "https://narmesteleder.nav.no/arbeidsgiver/ansatte";
    }

    public boolean erNærmesteLederForNoen() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Bearer " + tokenUtils.getTokenForInnloggetBruker());
        ResponseEntity<DigisyfoNarmesteLederRespons> responseEntity = restTemplate.exchange(
                syfoNarmesteLederUrl,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                DigisyfoNarmesteLederRespons.class
        );
        DigisyfoNarmesteLederRespons body = responseEntity.getBody();
        if (body == null) { // dette skal egentlig ikke kunne skje, men det gjør det ¯\_(ツ)_/¯
            log.warn("null response fra {}. {}", syfoNarmesteLederUrl, responseEntity);
            return false;
        }
        return !body.getAnsatte().isEmpty();
    }

}