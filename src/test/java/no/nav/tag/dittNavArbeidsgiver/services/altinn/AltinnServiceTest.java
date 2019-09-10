package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8082"})
public class AltinnServiceTest {

    @Autowired
    private AltinnService altinnService;

    @Test
    public void hentOrganisasjoner__skal_fungere_med_gyldig_fnr() {
        altinnService.hentOrganisasjoner("00000000000");
    }

    @Test(expected = AltinnException.class)
    public void hentOrganisasjoner__skal_kaste_altinn_exception_hvis_ugyldig_fnr() {
         altinnService.hentOrganisasjoner("04010100655");
    }

    @Test
    public void hentRoller__skal_fungere_med_gyldig_fnr_og_orgno() {
        altinnService.hentRoller("00000000000","000000000");
    }
    @Test
    public void henttilgangTilSkjemForBedrift() {
        List<Organisasjon> respons= altinnService.hentOrganisasjonerBasertPaRettigheter("01065500791", "4936", "1");
        System.out.println(respons);

    }

}
