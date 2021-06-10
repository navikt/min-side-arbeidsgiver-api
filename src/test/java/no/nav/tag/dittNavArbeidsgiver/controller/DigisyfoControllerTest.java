package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.tag.dittNavArbeidsgiver.services.digisyfo.DigisyfoServiceGcpImpl;
import org.junit.Before;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"mock.port=8083"})
public class DigisyfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private TokenValidationContextHolder requestContextHolder;

    @Mock
    private DigisyfoServiceGcpImpl digisyfoServiceImpl;

    @MockBean
    private DigisyfoController digisyfoController;

    @Before
    public void setUp() {
        digisyfoController = new DigisyfoController(digisyfoServiceImpl);

    }

    @Test
    public void testAtProtectedAnnotasjonForerTilsetOIDCValidationContext() {
        try {
            this.mockMvc.perform(get("/api/narmesteleder")).andExpect(status().isUnauthorized());
        } catch (Exception e) {
            e.printStackTrace();
        }
        verify(requestContextHolder).setTokenValidationContext(null);
    }
}