package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
public class AltinnServiceTest {

    @Autowired
    private AltinnService altinnService;

    @Test
    public void hentOrganisasjoner() {
        altinnService.hentOrganisasjoner("00000000000");
    }
    @Test(expected = AltinnException.class)
    public void hentOrganisasjoner_ugyldig_pnr() {
         altinnService.hentOrganisasjoner("04010100655");
    }

}
