package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import no.finn.unleash.Unleash;
import no.nav.security.oidc.context.TokenContext;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.junit.Before;
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
import static no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor.ISSUER_SELVBETJENING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8086"})
public class AltinnServiceMedAltinnProxyIntegrationTest {

    @MockBean
    private Unleash unleash;

    @MockBean
    private TokenUtils tokenUtils;


    @Autowired
    private AltinnService altinnService;

    @Before
    public void setUp() {
        when(unleash.isEnabled("arbeidsgiver.ditt-nav-arbeidsgiver-api.bruk-altinn-proxy")).thenReturn(true);
    }

    @Test
    public void hentOrganisasjoner__skal_fungere_med_gyldig_fnr() {
        setTokenContext(FNR_MED_ORGANISASJONER);

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

    @Test
    public void henttilgangTilSkjemForBedrift() {
        setTokenContext(FNR_MED_SKJEMATILGANG);

        List<Organisasjon> organisasjoner= altinnService.hentOrganisasjonerBasertPaRettigheter(
                FNR_MED_SKJEMATILGANG,
                SERVICE_CODE,
                SERVICE_EDITION
        );

        assertThat(organisasjoner).contains(
                Organisasjon.builder()
                        .name("JØA OG SEL")
                        .type("Business")
                        .parentOrganizationNumber("910020102")
                        .organizationNumber("910098896")
                        .organizationForm("BEDR")
                        .status("Active")
                        .build()
        );
    }


    private void setTokenContext(String fnr) {
        when(tokenUtils.getSelvbetjeningTokenContext()).thenReturn(
                new TokenContext(
                        ISSUER_SELVBETJENING,
                        fnr +"<--FNR_i_klar_tekst_for_MockServer")
        );
    }
}
