package no.nav.tag.dittNavArbeidsgiver.services.digisyfo;


import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.DigisyfoNarmesteLederRespons;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static no.nav.security.token.support.core.JwtTokenConstants.AUTHORIZATION_HEADER;

@Profile({"local", "labs", "dev-gcp", "prod-gcp"})
@Slf4j
@Service
@Setter
public class DigisyfoServiceGcpImpl implements DigisyfoService {

    private final RestTemplate restTemplate;
    private final TokenUtils tokenUtils;
    String syfoNarmesteLederUrl;

    @Autowired
    public DigisyfoServiceGcpImpl(RestTemplate restTemplate,
                                  TokenUtils tokenUtils,
                                  @Value("${digisyfo.narmestelederUrl}") String syfoNarmesteLederUrl) {
        this.restTemplate = restTemplate;
        this.tokenUtils = tokenUtils;
        this.syfoNarmesteLederUrl = syfoNarmesteLederUrl;
    }

    @Override
    public DigisyfoNarmesteLederRespons getNarmesteledere() {
        return restTemplate.exchange(
                syfoNarmesteLederUrl,
                HttpMethod.GET,
                getRequestEntity(),
                DigisyfoNarmesteLederRespons.class
        ).getBody();
    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Bearer " + tokenUtils.getTokenForInnloggetBruker());
        return new HttpEntity<>(headers);
    }


}
