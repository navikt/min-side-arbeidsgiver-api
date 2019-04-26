package no.nav.tag.dittNavArbeidsgiver.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.tag.dittNavArbeidsgiver.services.aktor.AktorClient;
import no.nav.tag.dittNavArbeidsgiver.utils.AccesstokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class DigisyfoService {

    private final AccesstokenClient accesstokenClient;
    private final AktorClient aktorClient;
    private final RestTemplate restTemplate;
    @Value("${digisyfo.digisyfoUrl}") private String digisyfoUrl;

    public DigisyfoService ( AccesstokenClient accesstokenClient, AktorClient aktorClient, RestTemplate restTemplate) {
        this.accesstokenClient = accesstokenClient;
        this.aktorClient = aktorClient;
        this.restTemplate = restTemplate;
    }

    public String getNarmesteledere(String fnr) {
            /*
            TODO: lagre accestoken fra AD (med en session?) så man slipper å spørre AD hver gang man skal sjekke nærmeste leder tilgang
             */
        HttpEntity<String> entity = getRequestEntity();
        String url = UriComponentsBuilder.fromHttpUrl(digisyfoUrl + aktorClient.getAktorId(fnr))
                .toUriString();
        try {
            ResponseEntity<String> respons = restTemplate.exchange(url,
                    HttpMethod.GET, entity, String.class);
            if(respons.getStatusCode() != HttpStatus.OK) {
                String message = "Kall mot digisyfo feiler med HTTP-" + respons.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);
            }
            return respons.getBody();
        } catch (RestClientException exception) {
            log.error(" Digisyfo Exception: ", exception);
            throw new RuntimeException(" Digisyfo Exception: "+ exception);
        }
    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(OIDCConstants.AUTHORIZATION_HEADER, "Bearer " + accesstokenClient.hentAccessToken().getAccess_token());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(headers);
    }
}
