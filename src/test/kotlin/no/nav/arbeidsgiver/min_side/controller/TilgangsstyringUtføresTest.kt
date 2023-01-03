package no.nav.arbeidsgiver.min_side.controller

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner::class)
@ActiveProfiles("local")
@TestPropertySource(properties = ["mock.port=8083"])
@EnableMockOAuth2Server
class TilgangsstyringUtføresTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun tilgangsstyringUtføres() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/api/narmesteleder/virksomheter-v2"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }
}