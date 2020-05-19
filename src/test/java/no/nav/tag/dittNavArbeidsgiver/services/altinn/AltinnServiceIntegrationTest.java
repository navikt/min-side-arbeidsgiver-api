package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import no.finn.unleash.Unleash;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.models.Role;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static no.nav.tag.dittNavArbeidsgiver.mockserver.MockServer.*;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8085"})
public class AltinnServiceIntegrationTest {

    @MockBean
    private Unleash unleash;

    @Autowired
    private AltinnService altinnService;

    @Test
    public void hentOrganisasjoner__skal_fungere_med_gyldig_fnr() {
        List<Organisasjon> organisasjoner = altinnService.hentOrganisasjoner(FNR_MED_ORGANISASJONER);
        assertThat(organisasjoner).contains(
                Organisasjon.builder()
                        .name("SKOTSELV OG HJELSET")
                        .type("Enterprise")
                        .organizationNumber("910720120")
                        .organizationForm("AS")
                        .status("Active")
                        .build()
        );
    }

    @Test(expected = AltinnException.class)
    public void hentOrganisasjoner__skal_kaste_altinn_exception_hvis_ugyldig_fnr() {
         altinnService.hentOrganisasjoner("11111111111");
    }

    @Test
    public void hentRoller__skal_fungere_med_gyldig_fnr_og_orgno() {
        List<Role> roller = altinnService.hentRoller(FNR_MED_ORGANISASJONER,"000000000");
        assertThat(roller).contains(Role.builder().roleId(272).roleType("Altinn").roleName("Primary industry and foodstuff").roleDescription("Import, processing, production and/or sales of primary products and other foodstuff").build());
    }

    @Test
    public void henttilgangTilSkjemForBedrift() {
        List<Organisasjon> organisasjoner= altinnService.hentOrganisasjonerBasertPaRettigheter(FNR_MED_SKJEMATILGANG, SERVICE_CODE, SERVICE_EDITION);
        assertThat(organisasjoner).contains(Organisasjon.builder().name("JØA OG SEL").type("Business").parentOrganizationNumber("910020102").organizationNumber("910098896").organizationForm("BEDR").status("Active").build());
    }

}
