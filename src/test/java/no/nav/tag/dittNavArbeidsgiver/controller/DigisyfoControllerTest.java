package no.nav.tag.dittNavArbeidsgiver.controller;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.finn.unleash.Unleash;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.services.digisyfo.DigisyfoService;
import no.nav.tag.dittNavArbeidsgiver.utils.GraphQlUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8083"})
public class DigisyfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private Unleash unleash;

    @SpyBean
    private OIDCRequestContextHolder requestContextHolder;

    @Mock
    private DigisyfoService digisyfoService;

    @MockBean
    private DigisyfoController digisyfoController;

    @Before
    public void setUp() {
        digisyfoController = new DigisyfoController(requestContextHolder, digisyfoService, unleash);

    }

    @Test
    public void sjekkNarmestelederTilgang() {

    }

    @Test
    public void hentAntallSykemeldinger() {
        when(unleash.isEnabled("dna.digisyfo.hentSykemeldinger")).thenReturn(false);
        String result = digisyfoController.hentAntallSykemeldinger("hei");
        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void testAtProtectedAnnotasjonForerTilsetOIDCValidationContext() {
        try {
            this.mockMvc.perform(get("/api/syfooppgaver")).andExpect(status().isUnauthorized());
        } catch (Exception e) {
            e.printStackTrace();
        }
        verify(requestContextHolder).setOIDCValidationContext(null);
    }
}