package no.nav.arbeidsgiver.min_side.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient


@SpringBootTest(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8118/faketokenx",
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
        val token = token(ACR.LEVEL4)
        mockMvc.get("/api/userInfo/v1") {
            header(AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun tilgangsstyringErOkForAcridportenLoaHigh() {
        val token = token(ACR.IDPORTEN_LOA_HIGH)
        mockMvc.get("/api/userInfo/v1") {
            header(AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    private fun token(acr: ACR): String =
        restClientBuilder
            .build()
            .post()
            .uri("http://localhost:8118/faketokenx/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                LinkedMultiValueMap<String, Any>().apply {
                    add("audience", "someaudience")
                    add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                    add("client_id", "fake")
                    add("client_secret", "fake")
                    add("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
                    add("subject_token", subjectToken)
                    add("acr", acr.value)
                }
            )
            .retrieve()
            .body(Map::class.java)?.get("access_token") as String
}

// subjectToken er hentet med en request mot fakedings: fakedings.intern.dev.nav.no/fake/idporten
private val subjectToken = "eyJraWQiOiJmYWtlIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJhdF9oYXNoIjoiNTVkODlmZjYtMjY4OS00ZWI4LWI1Y2UtNTY0MjkwNWMwMWJiIiwic3ViIjoiYWNiZGQwMmUtMDE5NS00YWIxLWEzODYtZDI5ZmZiNmIyYjVmIiwiYW1yIjpbIkJhbmtJRCJdLCJpc3MiOiJodHRwczovL2Zha2VkaW5ncy5pbnRlcm4uZGV2Lm5hdi5uby9mYWtlIiwicGlkIjoiMTIzNDU2Nzg5MTAiLCJsb2NhbGUiOiJuYiIsImNsaWVudF9pZCI6Im5vdGZvdW5kIiwidGlkIjoiZGVmYXVsdCIsInNpZCI6IjA5ZWExOWY5LTM5ODktNDM5NS1iNjE3LTJiYTA3Zjk0NWIwMyIsImF1ZCI6Im5vdGZvdW5kIiwiYWNyIjoiaWRwb3J0ZW4tbG9hLWhpZ2giLCJuYmYiOjE3MDA4NDkyNjIsImF6cCI6Im5vdGZvdW5kIiwiYXV0aF90aW1lIjoxNzAwODQ5MjYyLCJleHAiOjE3MDA4NTI4NjIsImlhdCI6MTcwMDg0OTI2MiwianRpIjoiYTYzNmVmYjMtMzgzMC00ZDU4LThmY2YtNjgwYTk3MGNjN2NlIn0.eS-FO_ty1NOANYu7IHq5mKzVjaPavpl9lrMTq7oclHJ1ymDKxpkgsslR0eLaZ6sIz9VlNPve36iIG-W_GPZmV8RvGFb6RFVORzIIKSeW1Hh3IdnRm_C5d0W_RZ48V8LolRWtM-CVs9XoVTrK9LYxfRSRz-oOTCVSjbTJVKubWNc-G7GLNpqyPM2x2pMmffMnNZz9wM_7-mxCE9KQ0lAHwS4I-xHMBYfsiezPEgmL9K-bdbMP7syMMop8BtWdaLU56VkuDO-W6DOl2plK1P9udR_h29QlHoVxwpQfUxefGbb7jeqOpVzIc45LMNEUlWxC00zfZNb3LBstopyl06ghJg"

/**
 * ifølge [doken](https://github.com/navikt/mock-oauth2-server) skal det være mulig å ha dynamiske verdier med templating i JSON_CONFIG.
 * Dette har jeg ikke fått til å fungere. Derfor er det en hardkodede verdier i JSON_CONFIG og en switch på acr requestParam
 */
private enum class ACR(val value: String) {
    LEVEL4("Level4"), IDPORTEN_LOA_HIGH("idporten-loa-high")
}

