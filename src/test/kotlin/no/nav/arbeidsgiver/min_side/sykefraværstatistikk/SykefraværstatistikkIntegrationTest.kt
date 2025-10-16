package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.provideDefaultObjectMapper
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykefraværStatistikkMetadataRecordProcessor
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykefraværStatistikkRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import java.time.LocalDateTime

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