package no.nav.arbeidsgiver.min_side.kontostatus

import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

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

    @MockBean
    lateinit var altinnService: AltinnService

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
            method(POST)
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

        mockMvc.post("/api/kontonummerStatus/v1") {
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
            method(POST)
        }.andRespond(withStatus(NOT_FOUND))

        kontoregisterClient.hentKontonummer(virksomhetsnummer).let {
            Assertions.assertNull(it)
        }

        mockMvc.post("/api/kontonummerStatus/v1") {
            content = """{"virksomhetsnummer": "$virksomhetsnummer"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "MANGLER_KONTONUMMER"}""") }
        }
    }

    @Test
    fun `henter kontonummer fra kontoregister og returnerer kontonummer og orgnr`() {
        val virksomhetsnummer = "42"
        server.expect {
            requestTo("/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer")
            method(POST)
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
        `when`(altinnService.harTilgang(virksomhetsnummer, "2896:87")).thenReturn(true)
        mockMvc.post("/api/kontonummer/v1") {
            content = """
                {
                    "orgnrForOppslag": "$virksomhetsnummer",
                    "orgnrForTilgangstyring": "$virksomhetsnummer"
                }
                """.trimIndent()
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "OK", "orgnr": "42", "kontonummer": "12345678901"}""") }
        }
    }

    @Test
    fun `bruker har ikke tilgang til å se kontonummer returnerer unauthorized`() {
        val virksomhetsnummer = "42"
        server.expect {
            requestTo("/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer")
            method(POST)
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
        `when`(altinnService.harTilgang(virksomhetsnummer, "2896:87")).thenReturn(false)
        mockMvc.post("/api/kontonummer/v1") {
            content = """
                {
                    "orgnrForOppslag": "$virksomhetsnummer",
                    "orgnrForTilgangstyring": "$virksomhetsnummer"
                }
                """.trimIndent()
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { null }
        }
    }

    @Test
    fun `kontnummer finnes ikke for virksomhet`() {
        val virksomhetsnummer = "123"
        server.expect {
            requestTo("/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer")
            method(POST)
        }.andRespond(withStatus(NOT_FOUND))

        kontoregisterClient.hentKontonummer(virksomhetsnummer).let {
            Assertions.assertNull(it)
        }

        `when`(altinnService.harTilgang(virksomhetsnummer, "2896:87")).thenReturn(true)

        mockMvc.post("/api/kontonummer/v1") {
            content = """
                {
                    "orgnrForOppslag": "$virksomhetsnummer",
                    "orgnrForTilgangstyring": "$virksomhetsnummer"
                }
                """.trimIndent()
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("123"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "MANGLER_KONTONUMMER", "orgnr":  null, "kontonummer": null}""") }
        }
    }
}