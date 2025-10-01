//package no.nav.arbeidsgiver.min_side.tiltak
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
//import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
//import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
//import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusKafkaListener
//import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
//import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.TJENESTEKODE
//import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.TJENESTEVERSJON
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.flywaydb.core.Flyway
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito.`when`
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.security.oauth2.jwt.JwtDecoder
//import org.springframework.test.context.bean.override.mockito.MockitoBean
//import org.springframework.test.json.JsonCompareMode.STRICT
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.get
//import java.util.UUID.randomUUID
//
//// TODO: Kafka
//@SpringBootTest(
//    properties = [
//        "server.servlet.context-path=/",
//        "spring.flyway.cleanDisabled=false",
//    ]
//)
//@AutoConfigureMockMvc
//class RefusjonStatusIntegrationTest {
//    @Autowired
//    lateinit var mockMvc: MockMvc
//
//    @Autowired
//    lateinit var refusjonStatusRepository: RefusjonStatusRepository
//
//    @Autowired
//    lateinit var objectMapper: ObjectMapper
//
//    lateinit var refusjonStatusKafkaListener: RefusjonStatusKafkaListener
//
//    @MockitoBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
//    lateinit var jwtDecoder: JwtDecoder
//
//    @MockitoBean
//    lateinit var altinnService: AltinnService
//
//    @Autowired
//    lateinit var flyway: Flyway
//
//    @BeforeEach
//    fun setup() {
//        flyway.clean()
//        flyway.migrate()
//        refusjonStatusKafkaListener = RefusjonStatusKafkaListener(
//            objectMapper,
//            refusjonStatusRepository
//        )
//    }
//
//
//    @Test
//    fun `statusoversikt returnerer riktig innhold`() {
//        `when`(
//            altinnService.hentAltinnTilganger()
//        ).thenReturn(
//            AltinnTilganger(
//                isError = false,
//                hierarki = listOf(
//                    AltinnTilganger.AltinnTilgang(
//                        orgnr = "1",
//                        altinn3Tilganger = setOf(),
//                        altinn2Tilganger = setOf(),
//                        underenheter = listOf(
//                            AltinnTilganger.AltinnTilgang(
//                                orgnr = "314",
//                                altinn3Tilganger = setOf(),
//                                altinn2Tilganger = setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
//                                underenheter = listOf(),
//                                navn = "Foo & Co",
//                                organisasjonsform = "BEDR"
//                            ),
//                            AltinnTilganger.AltinnTilgang(
//                                orgnr = "315",
//                                altinn3Tilganger = setOf(),
//                                altinn2Tilganger = setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
//                                underenheter = listOf(),
//                                navn = "Bar ltd.",
//                                organisasjonsform = "BEDR"
//                            ),
//                        ),
//                        navn = "overenhet",
//                        organisasjonsform = "AS"
//                    ),
//                ),
//                orgNrTilTilganger = mapOf(
//                    "314" to setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
//                    "315" to setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
//                ),
//                tilgangTilOrgNr = mapOf(
//                    "$TJENESTEKODE:$TJENESTEVERSJON" to setOf("314", "315"),
//                )
//            )
//        )
//        processRefusjonStatus("314", "ny")
//        processRefusjonStatus("314", "gammel")
//        processRefusjonStatus("314", "gammel")
//        processRefusjonStatus("315", "ny")
//        processRefusjonStatus("315", "ny")
//        processRefusjonStatus("315", "gammel")
//
//
//        // fjernes og erstattes av userinfo-endepunktet under
//        mockMvc.get("/api/refusjon_status") {
//            with(jwtWithPid("42"))
//        }.andExpect {
//            status { isOk() }
//            content {
//                json(
//                    """
//                    [
//                      {
//                        "virksomhetsnummer": "314",
//                        "statusoversikt": {
//                          "ny": 1,
//                          "gammel": 2
//                        },
//                        "tilgang": true
//                      },
//                      {
//                        "virksomhetsnummer": "315",
//                        "statusoversikt": {
//                          "ny": 2,
//                          "gammel": 1
//                        },
//                        "tilgang": true
//                      }
//                    ]
//                    """, STRICT
//                )
//            }
//        }
//
//        mockMvc.get("/api/userInfo/v3") {
//            with(jwtWithPid("42"))
//        }.asyncDispatch().andExpect {
//            status { isOk() }
//            content {
//                json(
//                    """
//                    {
//                      refusjoner: [
//                        {
//                          "virksomhetsnummer": "314",
//                          "statusoversikt": {
//                            "ny": 1,
//                            "gammel": 2
//                          },
//                          "tilgang": true
//                        },
//                        {
//                          "virksomhetsnummer": "315",
//                          "statusoversikt": {
//                            "ny": 2,
//                            "gammel": 1
//                          },
//                          "tilgang": true
//                        }
//                      ]
//                    }
//                    """
//                )
//            }
//        }
//
//
//    }
//
//    fun processRefusjonStatus(vnr: String, status: String) {
//        refusjonStatusKafkaListener.processConsumerRecord(
//            ConsumerRecord(
//                "", 0, 0, "",
//                """
//                {
//                    "refusjonId": "${randomUUID()}",
//                    "bedriftNr": "$vnr",
//                    "avtaleId": "${randomUUID()}",
//                    "status": "$status"
//                }
//                """
//            )
//        )
//    }
//}