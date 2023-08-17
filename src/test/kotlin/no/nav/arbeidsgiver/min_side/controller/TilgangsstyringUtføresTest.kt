package no.nav.arbeidsgiver.min_side.controller

import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = ["mock.port=8083"])
@EnableMockOAuth2Server
class TilgangsstyringUtføresTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    @Test
    fun tilgangsstyringUtføres() {
        mockMvc
            .perform(get("/api/narmesteleder/virksomheter-v3"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun tilgangsstyringErOkForAcrLevel4() {
        val token = token("42", mapOf("acr" to "Level4"))
        mockMvc
            .perform(get("/api/innlogget").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun tilgangsstyringErOkForAcridportenLoaHigh() {
        val token = token("42", mapOf("acr" to "idporten-loa-high"))
        mockMvc
            .perform(get("/api/innlogget").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun token(subject: String, claims: Map<String, String>): String? {
        return server.issueToken(
            "issuer1",
            "theclientid",
            DefaultOAuth2TokenCallback(
                "issuer1",
                subject,
                JOSEObjectType.JWT.type,
                listOf("someaudience"),
                claims + mapOf("pid" to subject),
                3600
            )
        ).serialize()
    }
}