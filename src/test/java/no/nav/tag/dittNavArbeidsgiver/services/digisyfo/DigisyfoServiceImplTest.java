package no.nav.tag.dittNavArbeidsgiver.services.digisyfo;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import no.nav.tag.dittNavArbeidsgiver.models.DigisyfoNarmesteLederRespons;


@RunWith(MockitoJUnitRunner.class)
public class DigisyfoServiceImplTest {

    private static final String MOCKSELVBETJENINGSTOKEN = "MOCKSELVBETJENINGSTOKEN";
    private static final String SYFO_URL = "http://test?status=ACTIVE";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TokenUtils tokenUtils;

    @InjectMocks
    private DigisyfoServiceGcpImpl digisyfoServiceImpl;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(digisyfoServiceImpl, "syfoNarmesteLederUrl", SYFO_URL);
        when(tokenUtils.getTokenForInnloggetBruker()).thenReturn(MOCKSELVBETJENINGSTOKEN);
    }

    @Test
    public void getNarmesteledere_skal_legge_paa_selvbetjeningstoken_og_returnere_svar_fra_Digisyfo() {
        DigisyfoNarmesteLederRespons respons = new DigisyfoNarmesteLederRespons();
        when(restTemplate.exchange(eq(SYFO_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class)))
                .thenReturn(ResponseEntity.ok(respons));
        assertThat(digisyfoServiceImpl.getNarmesteledere()).isSameAs(respons);
        verify(restTemplate).exchange(eq(SYFO_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class));
        verify(tokenUtils, times(1)).getTokenForInnloggetBruker();
        verifyNoMoreInteractions(restTemplate);
    }

    @Test(expected = RuntimeException.class)
    public void getNarmesteledere_skal_kaste_exception_dersom_syfo_ikke_svarer_http_ok() {
        when(restTemplate.exchange(eq(SYFO_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class)))
                .thenReturn(ResponseEntity.badRequest().build());
        try {
            digisyfoServiceImpl.getNarmesteledere();
        } catch (Exception e) {
            //Må catche exception her for å kunne gjøre verifiseringer
            verify(restTemplate).exchange(eq(SYFO_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class));
            verifyNoMoreInteractions(restTemplate);
            throw (e);
        }

    }
}
