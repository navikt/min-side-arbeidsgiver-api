package no.nav.tag.dittNavArbeidsgiver.services.yrkeskode;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Yrkeskoderespons.Yrkeskoderespons;
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
    private final Yrkeskoderespons yrkeskodeBeskrivelser;

    @Value("${yrkeskodeverk.yrkeskodeUrl}") private String yrkeskodeUrl;

    @Autowired
    public KodeverkService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", DittNavArbeidsgiverApplication.APP_NAME);
        this.yrkeskodeBeskrivelser = hentBetydningerAvYrkeskoder();
        this.headerEntity = new HttpEntity<>(headers);

    }

    public String finnYrkeskodebetydnning(String yrkeskodenokkel) {
        int kode = this.yrkeskodeBeskrivelser.getBetydninger().get(yrkeskodenokkel).indexOf(yrkeskodenokkel);
        return this.yrkeskodeBeskrivelser.getBetydninger().get(yrkeskodenokkel).get(kode).getBeskrivelser().getNn().getTekst();
    }

    public Yrkeskoderespons hentBetydningerAvYrkeskoder() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.set("Nav-Consumer-Id", DittNavArbeidsgiverApplication.APP_NAME);
        ResponseEntity <Yrkeskoderespons> respons = restTemplate.exchange(
                yrkeskodeUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Yrkeskoderespons.class
                );

        if (respons.getStatusCode() != HttpStatus.OK) {
            String message = "Kall mot kodeverksoversikt feiler med HTTP-" + respons.getStatusCode();
            log.error(message);
            throw new RuntimeException(message);
        }
        System.out.println("respons getbody" + respons.getBody());
        System.out.println("yrkekodemap er tom" + respons.getBody().getBetydninger().isEmpty());
        System.out.println("betydninger 1227184" + respons.getBody().getBetydninger().get("1227184"));
        return respons.getBody();
    }
}
