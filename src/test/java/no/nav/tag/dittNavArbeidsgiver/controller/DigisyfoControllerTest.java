package no.nav.tag.dittNavArbeidsgiver.controller;

import no.finn.unleash.Unleash;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.tag.dittNavArbeidsgiver.services.digisyfo.DigisyfoService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DigisyfoControllerTest {

    @Mock
    private Unleash unleash;

    @Mock
    private OIDCRequestContextHolder requestContextHolder;

    @Mock
    private DigisyfoService digisyfoService;

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
    public void hentSyfoOppgaver() {
    }
}