package no.nav.tag.dittNavArbeidsgiver.services.pdl;


import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;


import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Data;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Error;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.HentPerson;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.Navn;
import no.nav.tag.dittNavArbeidsgiver.utils.GraphQlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import no.nav.tag.dittNavArbeidsgiver.services.sts.STSClient;
import no.nav.tag.dittNavArbeidsgiver.services.sts.STStoken;
import no.nav.tag.dittNavArbeidsgiver.models.pdlPerson.PdlRespons;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@RunWith(MockitoJUnitRunner.class)
public class PdlServiceTest {
    private static final String FNR = "123";
    private static final String PDL_URL = "http://test";

    PdlRespons respons;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PdlService pdlService;

    @Mock
    STSClient stsClient;

    @Mock
    private GraphQlUtils graphQlUtils;

    @Before
    public void setUp() {
        pdlService.pdlUrl = PDL_URL;
        when(stsClient.getToken()).thenReturn(new STStoken());
        respons = new PdlRespons();
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
    public void hentNavnMedFnr_skal_hente_sts_token_og_returnere_navn_på_person() throws ExecutionException, InterruptedException {
        String navn = "Ole Dole";
        when(restTemplate.postForObject(eq(PDL_URL), any(HttpEntity.class), eq(PdlRespons.class)))
                .thenReturn(respons);
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo(navn);
        verify(stsClient).getToken();
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_og_returnere_ikke_funnet_person() throws ExecutionException, InterruptedException {
        PdlRespons tomRespons = new PdlRespons();
        Error ingenPersonError = new Error();
        ingenPersonError.message = "Fant ikke Person";
        tomRespons.data = new Data();
        tomRespons.data.hentPerson =  null;
        tomRespons.errors = new ArrayList<>();
        tomRespons.errors.add( ingenPersonError);
        when(restTemplate.postForObject(eq(PDL_URL), any(HttpEntity.class), eq(PdlRespons.class)))
                .thenReturn(tomRespons);
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo("Kunne ikke hente navn");
        verify(stsClient).getToken();
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_og_returnere_ikke_funnet_person_v_helt_tomPdlRespons() throws ExecutionException, InterruptedException {
        PdlRespons tomRespons = new PdlRespons();
        when(restTemplate.postForObject(eq(PDL_URL), any(HttpEntity.class), eq(PdlRespons.class)))
                .thenReturn(tomRespons);
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo("Kunne ikke hente navn");
        verify(stsClient).getToken();
    }

    @Test
    public void hentNavnMedFnr_skal_hente_sts_token_fange_opp_feil() throws ExecutionException, InterruptedException {
        when(restTemplate.postForObject(eq(PDL_URL), any(HttpEntity.class), eq(PdlRespons.class))).thenThrow(new RestClientException("401"));
        assertThat(pdlService.hentNavnMedFnr(FNR)).isEqualTo("Kunne ikke hente navn");
        verify(stsClient).getToken();
    }
}