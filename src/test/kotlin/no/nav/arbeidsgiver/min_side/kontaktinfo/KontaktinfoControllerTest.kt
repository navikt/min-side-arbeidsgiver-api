package no.nav.arbeidsgiver.min_side.kontaktinfo

import no.nav.arbeidsgiver.min_side.config.SecurityConfig
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor

@MockBean(JwtDecoder::class)
@WebMvcTest(
    value = [
        KontaktinfoController::class,
        SecurityConfig::class,
        AuthenticatedUserHolder::class,
    ],
    properties = [
        "server.servlet.context-path=/"
    ]
)
class KontaktinfoControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun protocolFormat() {
        mockMvc.kontaktinfo(
            content = """{ "virksomhetsnummer": "123456789" }"""
        ).andExpect {
            status { isOk() }
            content {
                contentType(APPLICATION_JSON)
                json("""
                    {
                        "hovedenhet": null,
                        "underenhet": null
                    }
                """.trimIndent())
            }
        }
    }

    @Test
    fun virksomhetsnummerAsNumberFails() {
        /* spring's objectmapper konverterer numbers til strings. */
        mockMvc.kontaktinfo(
            content = """{ "virksomhetsnummer": 123456789 }"""
        ).andExpect {
            status { isOk() }
        }
    }


    @Test
    fun wrongJsonInRequest() {
        mockMvc.kontaktinfo(
            content = """{  }"""
        ).andExpect {
            status { isBadRequest() }
        }
    }


    @Test
    fun superflousJsonFields() {
        /* spring's objectmapper godtar ekstra felter. */
        mockMvc.kontaktinfo(
            content = """{ "virksomhetsnummer": "12341234", "garbage": 2 }"""
        ).andExpect {
            status { isOk() }
        }
    }

    @Test
    fun disallowAcceptXML() {
        mockMvc.kontaktinfo(
            content = """{ "virksomhetsnummer": "12341234" }""",
            accept = MediaType.APPLICATION_XML
        ).andExpect {
            status { is4xxClientError() }
        }
    }

    private fun MockMvc.kontaktinfo(
        contentType: MediaType? = APPLICATION_JSON,
        content: String,
        auth: RequestPostProcessor = jwtWithPid("42"),
        accept: MediaType? = APPLICATION_JSON,
    ): ResultActionsDsl =
        post("/api/kontaktinfo/v1") {
            this.contentType = contentType
            this.content = content
            this.accept = accept
            with(auth)
        }
}