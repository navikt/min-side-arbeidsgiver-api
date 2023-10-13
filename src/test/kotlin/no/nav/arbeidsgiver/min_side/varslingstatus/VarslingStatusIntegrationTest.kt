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
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
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
    lateinit var kontaktInfoPollerRepository: KontaktInfoPollerRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    lateinit var varslingStatusKafkaListener: VarslingStatusKafkaListener

    @MockBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @MockBean
    lateinit var altinnService: AltinnService

    @Autowired
    lateinit var flyway: Flyway

    @Value("classpath:fager.ekstern-varsling-status.topic")
    lateinit var sampleTopic: Resource

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
                    "kvittertEventTimestamp": "2021-01-01T00:00:00Z",
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
                    "kvittertEventTimestamp": "2021-01-01T00:00:00Z",
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
            "MANGLER_KOFUVI" to "2021-01-02T00:00:00Z",
            "OK" to "2021-01-01T00:00:00Z",
            "MANGLER_KOFUVI" to "2021-01-04T00:00:00Z",
            "ANNEN_FEIL" to "2021-01-03T00:00:00Z",
        ).forEachIndexed { index, (status, timestamp) ->
            processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "314",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$timestamp",
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
                    "kvittertEventTimestamp": "2021-01-04T00:00:00Z"
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
            "MANGLER_KOFUVI" to "2021-01-01T00:00:00Z",
            "OK" to "2021-01-07T00:00:00Z",
            "ANNEN_FEIL" to "2021-01-02T00:00:00Z",
            "MANGLER_KOFUVI" to "2021-01-03T00:00:00Z",
        ).forEachIndexed { index, (status, timestamp) ->
            processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "314",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$timestamp",
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
                    "kvittertEventTimestamp": "2021-01-07T00:00:00Z"
                    }""",
                    true
                )
            }
        }
    }

    @Test
    fun `får ok dersom kontaktinfo er pollet og funnet`() {
        `when`(
            altinnService.hentOrganisasjoner("42")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "314", name = "Foo & Co")))

        processVarslingStatus(
            """
                {
                    "virksomhetsnummer": "314",
                    "varselId": "vid1",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "kvittertEventTimestamp": "2021-01-01T00:00:00Z",
                    "status": "MANGLER_KOFUVI",
                    "version": "1"
                }
            """
        )
        kontaktInfoPollerRepository.updateKontaktInfo("314", true, true)

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

    /**
     * fil generert med:
     * kafka-console-consumer.sh --bootstrap-server $KAFKA_BROKERS --consumer.config $KAFKA_CONFIG/kafka.properties --topic fager.ekstern-varsling-status --formatter kafka.tools.DefaultMessageFormatter --property print.value=true --from-beginning --timeout-ms 30000 > fager.ekstern-varsling-status.topic
     */
    @Test
    fun `konsumerer innhold på topic fra dev`() {
        sampleTopic.file.readLines().forEach {
            processVarslingStatus(it)
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