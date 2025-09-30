//package no.nav.arbeidsgiver.min_side.sykefraværstatistikk
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import io.ktor.server.plugins.di.dependencies
//import no.nav.arbeidsgiver.min_side.FakeApi
//import no.nav.arbeidsgiver.min_side.FakeApplication
//import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
//import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
//import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
//import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
//import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.flywaydb.core.Flyway
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.RegisterExtension
//import org.mockito.Mockito.`when`
//import org.skyscreamer.jsonassert.JSONAssert.assertEquals
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.security.oauth2.jwt.JwtDecoder
//import org.springframework.test.context.bean.override.mockito.MockitoBean
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
//import java.time.LocalDateTime
//
//@SpringBootTest(
//    properties = [
//        "server.servlet.context-path=/",
//        "spring.flyway.cleanDisabled=false",
//    ]
//)
//@AutoConfigureMockMvc
//class SykefraværstatistikkIntegrationTest {
//    companion object { //TODO: KAFKA
//        @RegisterExtension
//        val app = FakeApplication(
//            addDatabase = true,
//        ) {
//            dependencies {
//                provide<SykefraværstatistikkRepository>(SykefraværstatistikkRepository::class)
//                provide<ObjectMapper>(ObjectMapper::class)
//            }
//        }
//
//        @RegisterExtension
//        val fakeApi = FakeApi()
//    }
//
//    @Autowired
//    lateinit var mockMvc: MockMvc
//
//    @Autowired
//    lateinit var sykefraværstatistikkRepository: SykefraværstatistikkRepository
//
//    @Autowired
//    lateinit var objectMapper: ObjectMapper
//
//    lateinit var sykefraværstatistikkKafkaListener: SykefraværstatistikkKafkaListener
//
//
//    @Suppress("unused")
//    @MockitoBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
//    lateinit var jwtDecoder: JwtDecoder
//
//    @MockitoBean
//    lateinit var altinnService: AltinnService
//
//    @Autowired
//    lateinit var flyway: Flyway
//
//    val innenværendeår = LocalDateTime.now().year
//
//    @BeforeEach
//    fun setup() {
//        flyway.clean()
//        flyway.migrate()
//        sykefraværstatistikkKafkaListener = SykefraværstatistikkKafkaListener(
//            sykefraværstatistikkRepository,
//            objectMapper,
//        )
//    }
//
//    @Test
//    fun `bruker som representerer virksomhet med tilgang får virksomhetstatistikk`() {
//        `when`(
//            altinnService.harTilgang(
//                "123",
//                "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"
//            )
//        ).thenReturn(true)
//        processStatistikkkategori(
//            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "123",
//                    "kategori": "VIRKSOMHET",
//                    "sistePubliserteKvartal": {
//                        "prosent": 3.15,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 3.14
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "123",
//                    "kategori": "VIRKSOMHET",
//                    "sistePubliserteKvartal": {
//                        "prosent": 2.15,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 2.14
//                    }
//                }
//            """
//        )
//
//
//        mockMvc
//            .perform(
//                get("/api/sykefravaerstatistikk/{orgnr}", "123")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals(
//                    """
//                    {
//                        "type": "VIRKSOMHET",
//                        "label": "123",
//                        "prosent": 3.14
//                    }
//                """, it, true
//                )
//            }
//    }
//
//    @Test
//    fun `bruker uten tilgang får statistikk for bransje`() {
//        `when`(altinnService.harTilgang("321", "3403:1")).thenReturn(true)
//        processMetadataVirksomhet(
//            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "orgnr": "123",
//                    "bransje": "Testing",
//                    "naring": "IT",
//                    "arstall": "$innenværendeår",
//                    "kvartal": "1"
//                }
//            """
//        )
//        processMetadataVirksomhet(
//            """{ "orgnr": "123", "arstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "orgnr": "123",
//                    "bransje": "Testing Gammel",
//                    "naring": "IT Gammel",
//                    "arstall": "${innenværendeår - 1}",
//                    "kvartal": "1"
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "BRANSJE", "kode": "Testing", "årstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "Testing",
//                    "kategori": "BRANSJE",
//                    "sistePubliserteKvartal": {
//                        "prosent": 3.15,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 3.14
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "BRANSJE", "kode": "Testing Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "Testing Gammel",
//                    "kategori": "BRANSJE",
//                    "sistePubliserteKvartal": {
//                        "prosent": 2.15,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 2.14
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "IT",
//                    "kategori": "NÆRING",
//                    "sistePubliserteKvartal": {
//                        "prosent": 3.16,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 3.17
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "NÆRING", "kode": "IT Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "IT Gammel",
//                    "kategori": "NÆRING",
//                    "sistePubliserteKvartal": {
//                        "prosent": 2.16,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 2.17
//                    }
//                }
//            """
//        )
//
//
//        mockMvc
//            .perform(
//                get("/api/sykefravaerstatistikk/{orgnr}", "123")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals(
//                    """
//                    {
//                        "type": "BRANSJE",
//                        "label": "Testing",
//                        "prosent": 3.14
//                    }
//                """, it, true
//                )
//            }
//    }
//
//    @Test
//    fun `bruker uten tilgang får statistikk for næring`() {
//        `when`(altinnService.harTilgang("321", "3403:1")).thenReturn(true)
//        processMetadataVirksomhet(
//            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "orgnr": "123",
//                    "bransje": "Testing",
//                    "naring": "IT",
//                    "arstall": "$innenværendeår",
//                    "kvartal": "1"
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "IT",
//                    "kategori": "NÆRING",
//                    "sistePubliserteKvartal": {
//                        "prosent": 3.15,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 3.14
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "IT",
//                    "kategori": "NÆRING",
//                    "sistePubliserteKvartal": {
//                        "prosent": 2.15,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 2.14
//                    }
//                }
//            """
//        )
//
//
//        mockMvc
//            .perform(
//                get("/api/sykefravaerstatistikk/{orgnr}", "123")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals(
//                    """
//                    {
//                        "type": "NÆRING",
//                        "label": "IT",
//                        "prosent": 3.14
//                    }
//                """, it, true
//                )
//            }
//    }
//
//
//    @Test
//    fun `bruker med tilgang får statistikk for bransje når virksomhet mangler`() {
//        `when`(altinnService.harTilgang("123", "3403:1")).thenReturn(true)
//        processMetadataVirksomhet(
//            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "orgnr": "123",
//                    "bransje": "Testing",
//                    "naring": "IT",
//                    "arstall": "$innenværendeår",
//                    "kvartal": "1"
//                }
//            """
//        )
//        processMetadataVirksomhet(
//            """{ "orgnr": "123", "arstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "orgnr": "123",
//                    "bransje": "Testing Gammel",
//                    "naring": "IT",
//                    "arstall": "${innenværendeår - 1}",
//                    "kvartal": "1"
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "BRANSJE", "kode": "Testing", "årstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "Testing",
//                    "kategori": "BRANSJE",
//                    "sistePubliserteKvartal": {
//                        "prosent": 3.15,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 3.14,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "BRANSJE", "kode": "Testing Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "Testing Gammel",
//                    "kategori": "BRANSJE",
//                    "sistePubliserteKvartal": {
//                        "prosent": 2.15,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 2.14,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "IT",
//                    "kategori": "NÆRING",
//                    "sistePubliserteKvartal": {
//                        "prosent": 3.16,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 3.17,
//                        "årstall": "$innenværendeår",
//                        "kvartal": "1"
//                    }
//                }
//            """
//        )
//        processStatistikkkategori(
//            """{ "kategori": "NÆRING", "kode": "IT Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
//            """
//                {
//                    "kode": "IT Gammel",
//                    "kategori": "NÆRING",
//                    "sistePubliserteKvartal": {
//                        "prosent": 2.16,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    },
//                    "siste4Kvartal": {
//                        "prosent": 2.17,
//                        "årstall": "${innenværendeår - 1}",
//                        "kvartal": "1"
//                    }
//                }
//            """
//        )
//
//
//        mockMvc
//            .perform(
//                get("/api/sykefravaerstatistikk/{orgnr}", "123")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals(
//                    """
//                    {
//                        "type": "BRANSJE",
//                        "label": "Testing",
//                        "prosent": 3.14
//                    }
//                """, it, true
//                )
//            }
//    }
//
//    @Test
//    fun `no content dersom statistikk mangler`() {
//        `when`(altinnService.harTilgang("123", "3403:1")).thenReturn(true)
//        processMetadataVirksomhet(
//            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
//            """
//                {
//                    "orgnr": "123",
//                    "bransje": "Testing",
//                    "naring": "IT",
//                    "arstall": "$innenværendeår",
//                    "kvartal": "1"
//                }
//            """
//        )
//
//
//        mockMvc
//            .perform(
//                get("/api/sykefravaerstatistikk/{orgnr}", "123")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isNoContent)
//    }
//
//    private fun processStatistikkkategori(key: String, value: String) {
//        sykefraværstatistikkKafkaListener.processStatistikkategori(
//            ConsumerRecord(
//                "", 0, 0, key, value
//            )
//        )
//    }
//
//    private fun processMetadataVirksomhet(key: String, value: String) {
//        sykefraværstatistikkKafkaListener.processMetadataVirksomhet(
//            ConsumerRecord(
//                "", 0, 0, key, value
//            )
//        )
//    }
//}