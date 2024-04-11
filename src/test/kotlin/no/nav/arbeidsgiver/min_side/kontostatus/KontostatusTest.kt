package no.nav.arbeidsgiver.min_side.kontostatus

import no.nav.arbeidsgiver.min_side.clients.azuread.AzureService
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.enabled=false",
    ]
)
@AutoConfigureMockMvc
class KontostatusTest {

    @MockBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @MockBean
    lateinit var azureService: AzureService

    @Autowired
    lateinit var kontoregisterClient: KontoregisterClient

    lateinit var server: MockRestServiceServer

    @Autowired
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        server = MockRestServiceServer.bindTo(kontoregisterClient.restTemplate).build()
    }


    @Test
    fun `henter kontonummer fra kontoregister`() {
        val virksomhetsnummer = "42"
        server.expect {
            requestTo("/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer")
            method(GET)
        }.andRespond(
            withSuccess(
                """
                {
                    "mottaker": "42",
                    "kontonr": "12345678901"
                }
                """,
                APPLICATION_JSON
            )
        )

        kontoregisterClient.hentKontonummer(virksomhetsnummer).let {
            Assertions.assertEquals("42", it?.mottaker)
            Assertions.assertEquals("12345678901", it?.kontonr)
        }

        mockMvc.get("/api/kontonummerStatus/v1") {
            content = """{"virksomhetsnummer": "$virksomhetsnummer"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "OK"}""") }
        }
    }

    @Test
    fun `finner ikke kontonummer for virksomhet`() {
        val virksomhetsnummer = "123"
        server.expect {
            requestTo("/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer")
            method(GET)
        }.andRespond(withStatus(NOT_FOUND))

        kontoregisterClient.hentKontonummer(virksomhetsnummer).let {
            Assertions.assertNull(it)
        }

        mockMvc.get("/api/kontonummerStatus/v1") {
            content = """{"virksomhetsnummer": "$virksomhetsnummer"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "MANGLER_KONTONUMMER"}""") }
        }
    }
}