package no.nav.arbeidsgiver.min_side.controller;

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"mock.port=8083"})
@EnableMockOAuth2Server
public class TilgangsstyringUtføresTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void tilgangsstyringUtføres() {
        try {
            this.mockMvc
                    .perform(get("/api/narmesteleder/virksomheter-v2"))
                    .andExpect(status().isUnauthorized());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}