package no.nav.tag.dittNavArbeidsgiver.services.digisyfo;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import no.nav.tag.dittNavArbeidsgiver.models.DigisyfoNarmesteLederRespons;
import no.nav.tag.dittNavArbeidsgiver.services.aad.AadAccessToken;
import no.nav.tag.dittNavArbeidsgiver.services.aad.AccesstokenClient;
import no.nav.tag.dittNavArbeidsgiver.services.aktor.AktorClient;

@RunWith(MockitoJUnitRunner.class)
public class DigisyfoServiceTest {

    private static final String AKTOERID = "aktoerid";
    private static final String FNR = "123";
    private static final String SYFO_URL = "http://test";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AktorClient aktorClient;

    @Mock
    private AccesstokenClient accesstokenClient;

    @InjectMocks
    private DigisyfoService digisyfoService;

    @Before
    public void setUp() {
        digisyfoService.digisyfoUrl = SYFO_URL;
        when(aktorClient.getAktorId(FNR)).thenReturn(AKTOERID);
        when(accesstokenClient.hentAccessToken()).thenReturn(new AadAccessToken());
    }

    @Test
    public void getNarmesteledere_skal_hente_aktørid_og_accessToken_og_returnere_svar_fra_Digisyfo() {
        DigisyfoNarmesteLederRespons respons = new DigisyfoNarmesteLederRespons();
        when(restTemplate.exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class)))
            .thenReturn(ResponseEntity.ok(respons));
        assertThat(digisyfoService.getNarmesteledere(FNR)).isSameAs(respons);

        verify(aktorClient).getAktorId(FNR);
        verify(accesstokenClient).hentAccessToken();
        verify(restTemplate).exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class));
        verifyNoMoreInteractions(aktorClient, accesstokenClient, restTemplate);
    }

    @Test
    public void getNarmesteledere_skal_evicte_token_og_prøve_igjen_dersom_syfo_feiler() {
        DigisyfoNarmesteLederRespons respons = new DigisyfoNarmesteLederRespons();
        when(restTemplate.exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class)))
            .thenThrow(RestClientException.class).thenReturn(ResponseEntity.ok(respons));
        assertThat(digisyfoService.getNarmesteledere(FNR)).isSameAs(respons);

        verify(accesstokenClient, times(2)).hentAccessToken();
        verify(accesstokenClient).evict();
        verify(restTemplate, times(2)).exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class));

    }

    @Test(expected=RuntimeException.class)
    public void getNarmesteledere_skal_kaste_exception_dersom_syfo_feiler_to_ganger() {
        when(restTemplate.exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class)))
            .thenThrow(RestClientException.class);

        try {
            digisyfoService.getNarmesteledere(FNR);
        } catch (Exception e) {
            //Må catche exception her for å kunne gjøre verifiseringer
            verify(accesstokenClient, times(2)).hentAccessToken();
            verify(accesstokenClient).evict();
            verify(restTemplate, times(2)).exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class));
            throw(e);
        }

    }

    @Test(expected=RuntimeException.class)
    public void getNarmesteledere_skal_kaste_exception_dersom_syfo_ikke_svarer_http_ok() {
        when(restTemplate.exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class)))
            .thenReturn(ResponseEntity.badRequest().build());

        try {
            digisyfoService.getNarmesteledere(FNR);
        } catch (Exception e) {
            //Må catche exception her for å kunne gjøre verifiseringer
            verify(aktorClient).getAktorId(FNR);
            verify(accesstokenClient).hentAccessToken();
            verify(restTemplate).exchange(eq(SYFO_URL + AKTOERID), eq(HttpMethod.GET), any(HttpEntity.class), eq(DigisyfoNarmesteLederRespons.class));
            verifyNoMoreInteractions(aktorClient, accesstokenClient, restTemplate);
            throw(e);
        }

    }
}
