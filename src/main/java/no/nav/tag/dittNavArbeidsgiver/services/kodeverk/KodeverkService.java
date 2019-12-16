package no.nav.tag.dittNavArbeidsgiver.services.kodeverk;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;



import org.springframework.http.*;
import lombok.Data;
import no.nav.tag.dittNavArbeidsgiver.DittNavArbeidsgiverApplication;


import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
@Setter

@Data
public class KodeverkService {

    private final RestTemplate restTemplate;
    String kodeverkUrl;

    private Betydninger hentBetydningerAvYrkeskoder(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", DittNavArbeidsgiverApplication.APP_NAME);
        ResponseEntity<Betydninger> respons = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Betydninger.class);

        if (respons.getStatusCode() != HttpStatus.OK) {
            String message = "Kall mot kodeverksoversikt feiler med HTTP-" + respons.getStatusCode();
            log.error(message);
            throw new RuntimeException(message);
        }
        return respons.getBody();
    }
}
