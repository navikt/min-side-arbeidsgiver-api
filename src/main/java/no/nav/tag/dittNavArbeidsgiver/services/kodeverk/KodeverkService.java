package no.nav.tag.dittNavArbeidsgiver.services.kodeverk;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import no.nav.tag.dittNavArbeidsgiver.DittNavArbeidsgiverApplication;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Setter
public class KodeverkService {
    private final RestTemplate restTemplate;
    private final HttpEntity<String> headerEntity;

    @Value("${yrkeskodeverk.yrkeskodeUrl}") private String yrkeskodeUrl;

    @Autowired
    public KodeverkService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", DittNavArbeidsgiverApplication.APP_NAME);
        this.headerEntity = new HttpEntity<>(headers);

    }

    public Betydninger hentBetydningerAvYrkeskoder() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", DittNavArbeidsgiverApplication.APP_NAME);
        ResponseEntity<Betydninger> respons = restTemplate.exchange(
                yrkeskodeUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Betydninger.class);

        if (respons.getStatusCode() != HttpStatus.OK) {
            String message = "Kall mot kodeverksoversikt feiler med HTTP-" + respons.getStatusCode();
            log.error(message);
            throw new RuntimeException(message);
        }
        log.info("objekt returnert: ", respons);
        return respons.getBody();
    }
}
