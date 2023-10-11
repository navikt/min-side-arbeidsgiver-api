package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
@AutoConfigureMockMvc
class VarslingStatusIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var varslingStatusRepository: VarslingStatusRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    lateinit var varslingStatusKafkaListener: VarslingStatusKafkaListener

    @MockBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @MockBean
    lateinit var altinnService: AltinnService

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
        varslingStatusKafkaListener = VarslingStatusKafkaListener(
            varslingStatusRepository,
            objectMapper,
        )
    }

    @Test
    fun `bruker som ikke har tilgang får status ok som default`() {
        `when`(
            altinnService.hentOrganisasjoner("42")
        ).thenReturn(emptyList())

        processVarslingStatus(
            """
                {
                    "virksomhetsnummer": "314",
                    "varselId": "vid1",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "eventTimestamp": "2021-01-01T00:00:00",
                    "status": "MANGLER_KOFUVI",
                    "version": "1"
                }
            """
        )

        mockMvc.post("/api/varslingStatus/v1") {
            content = """{"virksomhetsnummer": "314"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "OK"}""") }
        }
    }

    @Test
    fun `bruker med tilgang men ingen status i databasen får OK som default`() {
        `when`(
            altinnService.hentOrganisasjoner("42")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "314", name = "Foo & Co")))

        processVarslingStatus(
            """
                {
                    "virksomhetsnummer": "86",
                    "varselId": "vid1",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "eventTimestamp": "2021-01-01T00:00:00",
                    "status": "MANGLER_KOFUVI",
                    "version": "1"
                }
            """
        )

        mockMvc.post("/api/varslingStatus/v1") {
            content = """{"virksomhetsnummer": "314"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content { json("""{"status": "OK"}""") }
        }
    }

    @Test
    fun `returnerer siste status for virksomhet`() {
        `when`(
            altinnService.hentOrganisasjoner("42")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "314", name = "Foo & Co")))

        listOf(
            "MANGLER_KOFUVI" to "2021-01-02T00:00:00",
            "OK" to "2021-01-01T00:00:00",
            "MANGLER_KOFUVI" to "2021-01-04T00:00:00",
            "ANNEN_FEIL" to "2021-01-03T00:00:00",
        ).forEachIndexed { index, (status, timestamp) ->
            processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "314",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "eventTimestamp": "$timestamp",
                        "status": "$status",
                        "version": "1"
                    }
                """
            )
        }

        mockMvc.post("/api/varslingStatus/v1") {
            content = """{"virksomhetsnummer": "314"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """{
                    "status": "MANGLER_KOFUVI", 
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "eventTimestamp": "2021-01-04T00:00:00"
                    }""",
                    true
                )
            }
        }
    }

    @Test
    fun `returnerer siste status for virksomhet OK`() {
        `when`(
            altinnService.hentOrganisasjoner("42")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "314", name = "Foo & Co")))

        listOf(
            "MANGLER_KOFUVI" to "2021-01-01T00:00:00",
            "OK" to "2021-01-07T00:00:00",
            "ANNEN_FEIL" to "2021-01-02T00:00:00",
            "MANGLER_KOFUVI" to "2021-01-03T00:00:00",
        ).forEachIndexed { index, (status, timestamp) ->
            processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "314",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "eventTimestamp": "$timestamp",
                        "status": "$status",
                        "version": "1"
                    }
                """
            )
        }

        mockMvc.post("/api/varslingStatus/v1") {
            content = """{"virksomhetsnummer": "314"}"""
            contentType = APPLICATION_JSON
            accept = APPLICATION_JSON
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """{
                    "status": "OK", 
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "eventTimestamp": "2021-01-07T00:00:00"
                    }""",
                    true
                )
            }
        }
    }

    private fun processVarslingStatus(value: String) {
        varslingStatusKafkaListener.processVarslingStatus(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
}