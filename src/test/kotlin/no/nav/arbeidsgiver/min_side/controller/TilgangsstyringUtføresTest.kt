package no.nav.arbeidsgiver.min_side.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.client.RestClient
import org.springframework.web.client.body


@SpringBootTest(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/fake",
        "spring.security.oauth2.resourceserver.jwt.audiences=someaudience"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TilgangsstyringUtføresTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var restClientBuilder: RestClient.Builder

    @Test
    fun tilgangsstyringUtføres() {
        mockMvc.get("/api/userInfo/v1")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun tilgangsstyringErOkForAcrLevel4() {
        val token = token("42", "Level4")
        mockMvc.get("/api/userInfo/v1") {
            header(AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun tilgangsstyringErOkForAcridportenLoaHigh() {
        val token = token("42", "idporten-loa-high")
        mockMvc.get("/api/userInfo/v1") {
            header(AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    private fun token(pid: String, acr: String): String? =
        restClientBuilder.build().get().uri("http://localhost:8080/fake/tokenx?client_id=someclientid&aud=someaudience&pid=$pid&acr=$acr").retrieve().body()
}