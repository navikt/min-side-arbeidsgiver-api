package no.nav.tag.dittNavArbeidsgiver.services.aareg;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsgiver;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class AAregService {
    private final STSClient stsClient;
    private final RestTemplate restTemplate;
    @Value("${aareg.aaregArbeidsforhold}")
    private String aaregArbeidsforholdUrl;
    @Value("${aareg.aaregArbeidsgivere}")
    private String aaregArbeidsgivereUrl;

    public AAregService(STSClient stsClient, RestTemplate restTemplate) {
        this.stsClient = stsClient;
        this.restTemplate = restTemplate;
    }

    public OversiktOverArbeidsForhold hentArbeidsforhold(String orgnr,String juridiskEnheOrgnr, String idPortenToken) {
        String url = aaregArbeidsforholdUrl;
        HttpEntity <String> entity = getRequestEntity(orgnr,juridiskEnheOrgnr,idPortenToken);
        System.out.println("har laget request entity");
        try {
            ResponseEntity<OversiktOverArbeidsForhold> respons = restTemplate.exchange(url,
                    HttpMethod.GET, entity, OversiktOverArbeidsForhold.class);
            if (respons.getStatusCode() != HttpStatus.OK) {
                String message = "Kall mot aareg feiler med HTTP-" + respons.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);
            }
            System.out.println(" hentArbeidsforhold respons.getBody()" + respons.getBody());
            return respons.getBody();
        } catch (RestClientException exception) {
            log.error(" Aareg Exception: ", exception);
            throw new RuntimeException(" Aareg Exception: " + exception);
        }
    }
    public List<OversiktOverArbeidsgiver> hentArbeidsgiverefraRapporteringsplikig(String orgnr, String opplysningspliktig, String idPortenToken) {
        String url = aaregArbeidsgivereUrl;
        HttpEntity <String> entity = getRequestEntity(orgnr,opplysningspliktig,idPortenToken);
        System.out.println("har laget request entity");
        try {
            ResponseEntity<List<OversiktOverArbeidsgiver>> respons = restTemplate.exchange(url,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<OversiktOverArbeidsgiver>>() {
                    });
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

    private HttpEntity <String> getRequestEntity(String bedriftsnr, String juridiskEnhetOrgnr, String idPortenToken) {
        String appName= "srvditt-nav-arbeid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + idPortenToken);
        headers.set("Nav-Call-Id", appName);
        headers.set("Nav-Arbeidsgiverident", bedriftsnr);
        headers.set("Nav-Opplysningspliktigident",juridiskEnhetOrgnr);
        headers.set("Nav-Consumer-Token", stsClient.getToken().getAccess_token());
        return new HttpEntity<>(headers);
    }
}
