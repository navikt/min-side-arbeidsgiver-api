package no.nav.tag.dittNavArbeidsgiver.services.digisyfo;

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.tag.dittNavArbeidsgiver.models.DigisyfoNarmesteLederRespons;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import static no.nav.security.token.support.core.JwtTokenConstants.AUTHORIZATION_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


@RunWith(SpringRunner.class)
@RestClientTest(components = DigisyfoServiceGcpImpl.class, properties = {
        "digisyfo.narmestelederUrl=" + DigisyfoServiceImplTest.SYFO_URL
})
@AutoConfigureWebClient(registerRestTemplate=true)
public class DigisyfoServiceImplTest {

    private static final String MOCKSELVBETJENINGSTOKEN = "MOCKSELVBETJENINGSTOKEN";
    static final String SYFO_URL = "http://test?status=ACTIVE";

    @Autowired
    private DigisyfoServiceGcpImpl digisyfoServiceImpl;

    @Autowired
    private MockRestServiceServer server;

    @MockBean
    private TokenUtils tokenUtils;

    @SuppressWarnings("unused")
    @MockBean
    private MultiIssuerConfiguration multiIssuerConfiguration;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(digisyfoServiceImpl, "syfoNarmesteLederUrl", SYFO_URL);
        when(tokenUtils.getTokenForInnloggetBruker()).thenReturn(MOCKSELVBETJENINGSTOKEN);
    }

    @Test
    public void getNarmesteledere_skal_legge_paa_selvbetjeningstoken_og_returnere_svar_fra_Digisyfo() {
        server.expect(requestTo(SYFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION_HEADER, "Bearer " + MOCKSELVBETJENINGSTOKEN))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(digisyfoServiceImpl.getNarmesteledere()).isInstanceOf(DigisyfoNarmesteLederRespons.class);
        verify(tokenUtils, times(1)).getTokenForInnloggetBruker();
    }

    @Test(expected = HttpClientErrorException.BadRequest.class)
    public void getNarmesteledere_skal_kaste_exception_dersom_syfo_ikke_svarer_http_ok() {
        server.expect(requestTo(SYFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        digisyfoServiceImpl.getNarmesteledere();
    }
}
