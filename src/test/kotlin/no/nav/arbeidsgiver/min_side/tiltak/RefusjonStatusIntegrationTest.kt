package no.nav.arbeidsgiver.min_side.tiltak

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerResponse
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusKafkaListener
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.TJENESTEKODE
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.TJENESTEVERSJON
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID.randomUUID

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
@AutoConfigureMockMvc
class RefusjonStatusIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var refusjonStatusRepository: RefusjonStatusRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    lateinit var refusjonStatusKafkaListener: RefusjonStatusKafkaListener

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
        refusjonStatusKafkaListener = RefusjonStatusKafkaListener(
            objectMapper,
            refusjonStatusRepository
        )
    }


    @Test
    fun `statusoversikt returnerer riktig innhold`() {
        `when`(
            altinnService.hentAltinnTilganger()
        ).thenReturn(
            AltinnTilgangerResponse(
                isError = false,
                hierarki = listOf(
                    AltinnTilgangerResponse.AltinnTilgang(
                        orgNr = "1",
                        altinn3Tilganger = setOf(),
                        altinn2Tilganger = setOf(),
                        underenheter = listOf(
                            AltinnTilgangerResponse.AltinnTilgang(
                                orgNr = "314",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                                underenheter = listOf(),
                                name = "Foo & Co",
                                organizationForm = "BEDR"
                            ),
                            AltinnTilgangerResponse.AltinnTilgang(
                                orgNr = "315",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                                underenheter = listOf(),
                                name = "Bar ltd.",
                                organizationForm = "BEDR"
                            ),
                        ),
                        name = "overenhet",
                        organizationForm = "AS"
                    ),
                ),
                orgNrTilTilganger = mapOf(
                    "314" to setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                    "315" to setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                ),
                tilgangTilOrgNr = mapOf(
                    "$TJENESTEKODE:$TJENESTEVERSJON" to setOf("314", "315"),
                )
            )
        )
        processRefusjonStatus("314", "ny")
        processRefusjonStatus("314", "gammel")
        processRefusjonStatus("314", "gammel")
        processRefusjonStatus("315", "ny")
        processRefusjonStatus("315", "ny")
        processRefusjonStatus("315", "gammel")


        // fjernes og erstattes av userinfo-endepunktet under
        mockMvc.get("/api/refusjon_status") {
            with(jwtWithPid("42"))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    [
                      {
                        "virksomhetsnummer": "314",
                        "statusoversikt": {
                          "ny": 1,
                          "gammel": 2
                        },
                        "tilgang": true
                      },
                      {
                        "virksomhetsnummer": "315",
                        "statusoversikt": {
                          "ny": 2,
                          "gammel": 1
                        },
                        "tilgang": true
                      }
                    ]
                    """, true
                )
            }
        }

        mockMvc.get("/api/userInfo/v1") {
            with(jwtWithPid("42"))
        }.asyncDispatch().andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      refusjoner: [
                        {
                          "virksomhetsnummer": "314",
                          "statusoversikt": {
                            "ny": 1,
                            "gammel": 2
                          },
                          "tilgang": true
                        },
                        {
                          "virksomhetsnummer": "315",
                          "statusoversikt": {
                            "ny": 2,
                            "gammel": 1
                          },
                          "tilgang": true
                        }
                      ]
                    }
                    """
                )
            }
        }


    }

    fun processRefusjonStatus(vnr: String, status: String) {
        refusjonStatusKafkaListener.processConsumerRecord(
            ConsumerRecord(
                "", 0, 0, "",
                """
                { 
                    "refusjonId": "${randomUUID()}", 
                    "bedriftNr": "$vnr", 
                    "avtaleId": "${randomUUID()}", 
                    "status": "$status"
                }
                """
            )
        )
    }
}