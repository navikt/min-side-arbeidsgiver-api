package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.di.dependencies
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.provideDefaultObjectMapper
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.digisyfo.ConsumerRecordProcessor
import no.nav.arbeidsgiver.min_side.services.digisyfo.MsaKafkaConsumer
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykefraværStatistikkMetadataRecordProcessor
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykefraværStatistikkRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

//@SpringBootTest(
//    properties = [
//        "server.servlet.context-path=/",
//        "spring.flyway.cleanDisabled=false",
//    ]
//)
//@AutoConfigureMockMvc
class SykefraværstatistikkIntegrationTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<SykefraværstatistikkService>(SykefraværstatistikkService::class)
                provide<SykefraværstatistikkRepository>(SykefraværstatistikkRepository::class)
                provide<AltinnService> { Mockito.mock<AltinnService>() }
                provideDefaultObjectMapper()
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

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

    val innenværendeår = LocalDateTime.now().year

    @Test
    fun `bruker som representerer virksomhet med tilgang får virksomhetstatistikk`() = app.runTest {
        val token = fakeToken("42")

        `when`(
            app.getDependency<AltinnService>().harTilgang(
                "123",
                "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk",
                token
            )
        ).thenReturn(true)
        app.processStatistikkkategori(
            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14
                    }
                }
            """
        )

        client.get("/api/sykefravaerstatistikk/123") {
            bearerAuth(token)
        }.also {
            assert(HttpStatusCode.OK == it.status)
            assertEquals(
                """
                    {
                        "type": "VIRKSOMHET",
                        "label": "123",
                        "prosent": 3.14
                    }
                """, it.bodyAsText(), true
            )
        }

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
    }

    @Test
    fun `bruker uten tilgang får statistikk for bransje`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harTilgang("123", "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk", token)).thenReturn(false)
        app.processMetadataVirksomhet(
            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenværendeår",
                    "kvartal": "1"
                }
            """
        )
        app.processMetadataVirksomhet(
            """{ "orgnr": "123", "arstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing Gammel",
                    "naring": "IT Gammel",
                    "arstall": "${innenværendeår - 1}",
                    "kvartal": "1"
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "BRANSJE", "kode": "Testing", "årstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "BRANSJE", "kode": "Testing Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing Gammel",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.16,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.17
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "NÆRING", "kode": "IT Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "IT Gammel",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.16,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.17
                    }
                }
            """
        )


        client.get("/api/sykefravaerstatistikk/123") {
            bearerAuth(token)
        }.also {
            assert(HttpStatusCode.OK == it.status)
            assertEquals(
                """
                    {
                        "type": "BRANSJE",
                        "label": "Testing",
                        "prosent": 3.14
                    }
                """, it.bodyAsText(), true
            )
        }

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
    }

    @Test
    fun `bruker uten tilgang får statistikk for næring`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harTilgang("123", "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk", token)).thenReturn(false)
        app.processMetadataVirksomhet(
            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenværendeår",
                    "kvartal": "1"
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14
                    }
                }
            """
        )



        client.get("/api/sykefravaerstatistikk/123") {
            bearerAuth(token)
        }.also {
            assert(HttpStatusCode.OK == it.status)
            assertEquals(
                """
                    {
                        "type": "NÆRING",
                        "label": "IT",
                        "prosent": 3.14
                    }
                """, it.bodyAsText(), true
            )
        }
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
    }


    @Test
    fun `bruker med tilgang får statistikk for bransje når virksomhet mangler`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harTilgang("123", "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk", token)).thenReturn(true)
        app.processMetadataVirksomhet(
            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenværendeår",
                    "kvartal": "1"
                }
            """
        )
        app.processMetadataVirksomhet(
            """{ "orgnr": "123", "arstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing Gammel",
                    "naring": "IT",
                    "arstall": "${innenværendeår - 1}",
                    "kvartal": "1"
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "BRANSJE", "kode": "Testing", "årstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "BRANSJE", "kode": "Testing Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing Gammel",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.16,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.17,
                        "årstall": "$innenværendeår",
                        "kvartal": "1"
                    }
                }
            """
        )
        app.processStatistikkkategori(
            """{ "kategori": "NÆRING", "kode": "IT Gammel", "årstall": "${innenværendeår - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "IT Gammel",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.16,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.17,
                        "årstall": "${innenværendeår - 1}",
                        "kvartal": "1"
                    }
                }
            """
        )

        client.get("/api/sykefravaerstatistikk/123") {
            bearerAuth(token)
        }.also {
            assert(HttpStatusCode.OK == it.status)
            assertEquals(
                """
                    {
                        "type": "BRANSJE",
                        "label": "Testing",
                        "prosent": 3.14
                    }
                """, it.bodyAsText(), true
            )
        }

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
    }

    @Test
    fun `no content dersom statistikk mangler`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harTilgang("123", "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk", token)).thenReturn(true)
        app.processMetadataVirksomhet(
            """{ "orgnr": "123", "arstall": "$innenværendeår", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenværendeår",
                    "kvartal": "1"
                }
            """
        )

        client.get("/api/sykefravaerstatistikk/123") {
            bearerAuth(token)
        }.also {
            assert(HttpStatusCode.NoContent == it.status)
        }
//
//        mockMvc
//            .perform(
//                get("/api/sykefravaerstatistikk/{orgnr}", "123")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isNoContent)
    }

    private suspend fun FakeApplication.processStatistikkkategori(key: String, value: String) {
        val processor = SykefraværStatistikkRecordProcessor(
            getDependency<ObjectMapper>(),
            getDependency<SykefraværstatistikkRepository>()
        )
        processor.processRecord(
            ConsumerRecord(
                "", 0, 0, key, value
            )
        )
    }

    private suspend fun FakeApplication.processMetadataVirksomhet(key: String, value: String) {
        val processor = SykefraværStatistikkMetadataRecordProcessor(
            getDependency<ObjectMapper>(),
            getDependency<SykefraværstatistikkRepository>()
        )
        processor.processRecord(
            ConsumerRecord(
                "", 0, 0, key, value
            )
        )
    }
}