package no.nav.tag.dittNavArbeidsgiver.services.aareg;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service

public class AAregService {
    private final STSClient stsClient;
    private final RestTemplate restTemplate;
    @Value("${aareg.aaregUrl}")
    private String aaregUrl;

    public AAregService(STSClient stsClient, RestTemplate restTemplate) {
        this.stsClient = stsClient;
        this.restTemplate = restTemplate;
    }

    public OversiktOverArbeidsForhold hentArbeidsforhold(String orgnr) {
        String url = aaregUrl;
        HttpEntity <String> entity = getRequestEntity(orgnr);
        try {
            ResponseEntity<OversiktOverArbeidsForhold> respons = restTemplate.exchange(url,
                    HttpMethod.GET, entity, OversiktOverArbeidsForhold.class);
            if (respons.getStatusCode() != HttpStatus.OK) {
                String message = "Kall mot aareg feiler med HTTP-" + respons.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);
            }
            return respons.getBody();
        } catch (RestClientException exception) {
            log.error(" Aareg Exception: ", exception);
            throw new RuntimeException(" Aareg Exception: " + exception);
        }
    }

    private HttpEntity <String> getRequestEntity(String orgnr) {
        String appName= "srvditt-nav-arbeid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + stsClient.getToken().getAccess_token());
        headers.set("Nav-Call-Id", appName);
        headers.set("Nav-Arbeidsgiverident", "910825496");
        headers.set("Nav-Opplysningspliktigident","810825472");
        headers.set("Nav-Consumer-Token", stsClient.getToken().getAccess_token());

        return new HttpEntity<>(headers);
    }

}
