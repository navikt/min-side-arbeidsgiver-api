package no.nav.tag.dittNavArbeidsgiver.services.pdl;


import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;


import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Data;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Error;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.HentPerson;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STStoken;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlPerson;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class PdlServiceTest {
    private static final String FNR = "123";
    private static final String PDL_URL = "http://test";

    @Mock
    PdlPerson respons;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PdlService pdlService;

    @Mock
    STSClient stsClient;

    @Before
    public void setUp() {
        pdlService.pdlUrl = PDL_URL;
        when(stsClient.getToken()).thenReturn(new STStoken());
        respons = new PdlPerson();
        lagPdlObjekt();
    }

    public void lagPdlObjekt(){
        respons.data = new Data();
        respons.data.hentPerson =  new HentPerson();
        respons.data.hentPerson.navn = new Navn[1];
        Navn testNavn = new Navn();
        testNavn.fornavn = "Ole";
        testNavn.etternavn = "Dole";
        respons.data.hentPerson.navn[0]=testNavn;
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_og_returnere_navn_på_person() {
        String navn = "Ole Dole";
        when(restTemplate.exchange(eq(PDL_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(PdlPerson.class)))
                .thenReturn(ResponseEntity.ok(respons));
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo(navn);
        verify(stsClient).getToken();
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_og_returnere_ikke_funnet_person() {
        PdlPerson tomRespons = new PdlPerson();
        Error ingenPersonError = new Error();
        ingenPersonError.message = "Fant ikke Person";
        tomRespons.data = new Data();
        tomRespons.data.hentPerson =  null;
        tomRespons.errors = new ArrayList<>();
        tomRespons.errors.add( ingenPersonError);
        when(restTemplate.exchange(eq(PDL_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(PdlPerson.class)))
                .thenReturn(ResponseEntity.ok(tomRespons));
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo("Kunne ikke hente navn");
        verify(stsClient).getToken();
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_og_returnere_ikke_funnet_person_v_helt_tomPdlRespons() {
        PdlPerson tomRespons = new PdlPerson();
        when(restTemplate.exchange(eq(PDL_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(PdlPerson.class)))
                .thenReturn(ResponseEntity.ok(tomRespons));
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo("Kunne ikke hente navn");
        verify(stsClient).getToken();
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_fange_opp_feil() {
        when(restTemplate.exchange(eq(PDL_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(PdlPerson.class)))
                .thenReturn(ResponseEntity.status(401).body(respons));
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo("Kunne ikke hente navn");
        verify(stsClient).getToken();
    }
}
