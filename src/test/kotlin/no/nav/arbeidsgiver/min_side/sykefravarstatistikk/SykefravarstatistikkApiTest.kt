package no.nav.arbeidsgiver.min_side.sykefravarstatistikk

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.AltinnTilgangerMock
import no.nav.arbeidsgiver.min_side.configureSykefravarstatistikkRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykefraværStatistikkMetadataRecordProcessor
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykefraværStatistikkRecordProcessor
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SykefravarstatistikkApiTest {
    val innenvarendear = LocalDateTime.now().year

    @Test
    fun `bruker som representerer virksomhet med tilgang får virksomhetstatistikk`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123",
                    ressurs = "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(SykefravarstatistikkRepository::class)
            provide(SykefraværStatistikkRecordProcessor::class)
            provide(SykefraværstatistikkService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureSykefravarstatistikkRoutes()
        }
    ) {
        val statistikkProcessor = resolve<SykefraværStatistikkRecordProcessor>()

        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14
                    }
                }
            """
        )

        client.get("ditt-nav-arbeidsgiver-api/api/sykefravaerstatistikk/123") {
            bearerAuth("faketoken")
        }.also {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
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
    fun `bruker uten tilgang får statistikk for bransje`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(AltinnTilgangerMock.empty)
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(SykefravarstatistikkRepository::class)
            provide(SykefraværStatistikkRecordProcessor::class)
            provide(SykefraværStatistikkMetadataRecordProcessor::class)
            provide(SykefraværstatistikkService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureSykefravarstatistikkRoutes()
        }
    ) {
        val metadataProcessor = resolve<SykefraværStatistikkMetadataRecordProcessor>()
        val statistikkProcessor = resolve<SykefraværStatistikkRecordProcessor>()

        metadataProcessor.processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenvarendear",
                    "kvartal": "1"
                }
            """
        )
        metadataProcessor.processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing Gammel",
                    "naring": "IT Gammel",
                    "arstall": "${innenvarendear - 1}",
                    "kvartal": "1"
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "BRANSJE", "kode": "Testing", "årstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "BRANSJE", "kode": "Testing Gammel", "årstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing Gammel",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.16,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.17
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "NÆRING", "kode": "IT Gammel", "årstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "IT Gammel",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.16,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.17
                    }
                }
            """
        )


        client.get("ditt-nav-arbeidsgiver-api/api/sykefravaerstatistikk/123") {
            bearerAuth("faketoken")
        }.also {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """
                    {
                        "type": "BRANSJE",
                        "label": "Testing",
                        "prosent": 3.14
                    }
                """,
                it.bodyAsText(),
                true
            )
        }
    }

    @Test
    fun `bruker uten tilgang får statistikk for næring`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(AltinnTilgangerMock.empty)
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(SykefravarstatistikkRepository::class)
            provide(SykefraværStatistikkRecordProcessor::class)
            provide(SykefraværStatistikkMetadataRecordProcessor::class)
            provide(SykefraværstatistikkService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureSykefravarstatistikkRoutes()
        }
    ) {
        val metadataProcessor = resolve<SykefraværStatistikkMetadataRecordProcessor>()
        val statistikkProcessor = resolve<SykefraværStatistikkRecordProcessor>()

        metadataProcessor.processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenvarendear",
                    "kvartal": "1"
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14
                    }
                }
            """
        )



        client.get("ditt-nav-arbeidsgiver-api/api/sykefravaerstatistikk/123") {
            bearerAuth("faketoken")
        }.also {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
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
    fun `bruker med tilgang får statistikk for bransje når virksomhet mangler`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123",
                    ressurs = "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(SykefravarstatistikkRepository::class)
            provide(SykefraværStatistikkRecordProcessor::class)
            provide(SykefraværStatistikkMetadataRecordProcessor::class)
            provide(SykefraværstatistikkService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureSykefravarstatistikkRoutes()
        }
    ) {
        val metadataProcessor = resolve<SykefraværStatistikkMetadataRecordProcessor>()
        val statistikkProcessor = resolve<SykefraværStatistikkRecordProcessor>()
        metadataProcessor.processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenvarendear",
                    "kvartal": "1"
                }
            """
        )
        metadataProcessor.processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing Gammel",
                    "naring": "IT",
                    "arstall": "${innenvarendear - 1}",
                    "kvartal": "1"
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "BRANSJE", "kode": "Testing", "årstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "BRANSJE", "kode": "Testing Gammel", "årstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "Testing Gammel",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "NÆRING", "kode": "IT", "årstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.16,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.17,
                        "årstall": "$innenvarendear",
                        "kvartal": "1"
                    }
                }
            """
        )
        statistikkProcessor.processRecordKeyValue(
            """{ "kategori": "NÆRING", "kode": "IT Gammel", "årstall": "${innenvarendear - 1}", "kvartal": "1" }""",
            """
                {
                    "kode": "IT Gammel",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.16,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.17,
                        "årstall": "${innenvarendear - 1}",
                        "kvartal": "1"
                    }
                }
            """
        )

        client.get("ditt-nav-arbeidsgiver-api/api/sykefravaerstatistikk/123") {
            bearerAuth("faketoken")
        }.also {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
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
    fun `no content dersom statistikk mangler`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123",
                    ressurs = "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(SykefravarstatistikkRepository::class)
            provide(SykefraværStatistikkMetadataRecordProcessor::class)
            provide(SykefraværstatistikkService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureSykefravarstatistikkRoutes()
        }
    ) {
        val metadataProcessor = resolve<SykefraværStatistikkMetadataRecordProcessor>()
        metadataProcessor.processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "$innenvarendear", "kvartal": "1" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "$innenvarendear",
                    "kvartal": "1"
                }
            """
        )

        client.get("ditt-nav-arbeidsgiver-api/api/sykefravaerstatistikk/123") {
            bearerAuth("faketoken")
        }.also {
            assertEquals(HttpStatusCode.NoContent, it.status)
        }
    }
}