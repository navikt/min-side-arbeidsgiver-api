package no.nav.tag.dittNavArbeidsgiver.services.aareg;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.tag.dittNavArbeidsgiver.models.DigisyfoNarmesteLederRespons;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.services.aktor.AktorClient;
import no.nav.tag.dittNavArbeidsgiver.utils.AccesstokenClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service

public class AAregService {
    private final AccesstokenClient accesstokenClient;
    private final RestTemplate restTemplate;
    @Value("${aareg.aaregUrl}")
    private String aaregUrl;

    public AAregService(AccesstokenClient accesstokenClient, RestTemplate restTemplate) {
        this.accesstokenClient = accesstokenClient;
        this.restTemplate = restTemplate;
    }

    public OversiktOverArbeidsForhold hentArbeidsforhold(String orgnr) {

        String url = aaregUrl;
        ResponseEntity<OversiktOverArbeidsForhold.ArbeidsForhold> respons = getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},url);
        log.info("Henter organisasjoner fra Altinn");
        return respons.getBody();
    }








}
