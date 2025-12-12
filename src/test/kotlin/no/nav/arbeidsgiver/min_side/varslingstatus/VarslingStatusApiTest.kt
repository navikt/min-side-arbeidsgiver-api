package no.nav.arbeidsgiver.min_side.varslingstatus

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.AltinnTilgangerMock
import no.nav.arbeidsgiver.min_side.configureVarslingStatusRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.digisyfo.VarslingStatusRecordProcessor
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class VarslingStatusApiTest {

    val token = "faketoken"

    @Test
    fun `bruker som ikke har tilgang får status ok som default`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(AltinnTilgangerMock.empty)
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == token) mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(VarslingStatusRepository::class)
            provide(VarslingStatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureVarslingStatusRoutes()
        }
    ) {
        val processor = VarslingStatusRecordProcessor(VarslingStatusRepository(database))

        processor.processRecordValue(
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

        client.post("ditt-nav-arbeidsgiver-api/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals("""{"status":"OK"}""", it.bodyAsText(), false)
        }
    }

    @Test
    fun `bruker med tilgang men ingen status i databasen får OK som default`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "314",
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == token) mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(VarslingStatusRepository::class)
            provide(VarslingStatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureVarslingStatusRoutes()
        }
    ) {
        val processor = VarslingStatusRecordProcessor(VarslingStatusRepository(database))

        processor.processRecordValue(
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

        client.post("ditt-nav-arbeidsgiver-api/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals("""{"status":"OK"}""", it.bodyAsText(), false)
        }
    }

    @Test
    fun `returnerer siste status for virksomhet`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "314",
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == token) mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(VarslingStatusRepository::class)
            provide(VarslingStatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureVarslingStatusRoutes()
        }
    ) {
        val processor = VarslingStatusRecordProcessor(VarslingStatusRepository(database))

        listOf(
            "MANGLER_KOFUVI" to "2021-01-02T00:00:00Z",
            "OK" to "2021-01-01T00:00:00Z",
            "MANGLER_KOFUVI" to "2021-01-04T00:00:00Z",
            "ANNEN_FEIL" to "2021-01-03T00:00:00Z",
        ).forEachIndexed { index, (status, timestamp) ->
            processor.processRecordValue(
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
        client.post("ditt-nav-arbeidsgiver-api/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """{
                    "status": "MANGLER_KOFUVI",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "kvittertEventTimestamp": "2021-01-04T00:00:00Z"
                    }""", it.bodyAsText(), true
            )
        }
    }


    @Test
    fun `returnerer siste status for virksomhet OK`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "314",
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == token) mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(VarslingStatusRepository::class)
            provide(VarslingStatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureVarslingStatusRoutes()
        }
    ) {
        val processor = VarslingStatusRecordProcessor(VarslingStatusRepository(database))

        listOf(
            "MANGLER_KOFUVI" to "2021-01-01T00:00:00Z",
            "OK" to "2021-01-07T00:00:00Z",
            "ANNEN_FEIL" to "2021-01-02T00:00:00Z",
            "MANGLER_KOFUVI" to "2021-01-03T00:00:00Z",
        ).forEachIndexed { index, (status, timestamp) ->
            processor.processRecordValue(
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

        client.post("ditt-nav-arbeidsgiver-api/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """{
                    "status": "OK",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "kvittertEventTimestamp": "2021-01-07T00:00:00Z"
                    }""", it.bodyAsText(), true
            )
        }
    }

    @Test
    fun `får ok dersom kontaktinfo er pollet og funnet`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "314",
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == token) mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(VarslingStatusRepository::class)
            provide(VarslingStatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureVarslingStatusRoutes()
        }
    ) {
        val repository = VarslingStatusRepository(database)
        val processor = VarslingStatusRecordProcessor(repository)
        processor.processRecordValue(
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

        KontaktInfoPollerRepository(database).updateKontaktInfo(
            virksomhetsnummer = "314",
            harEpost = true,
            harTlf = true
        )

        client.post("ditt-nav-arbeidsgiver-api/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """{"status": "OK"}""", it.bodyAsText(), false
            )
        }
    }

    /**
     * fil generert med:
     * kafka-console-consumer.sh --bootstrap-server $KAFKA_BROKERS --consumer.config $KAFKA_CONFIG/kafka.properties --topic fager.ekstern-varsling-status --formatter kafka.tools.DefaultMessageFormatter --property print.value=true --from-beginning --timeout-ms 30000 > fager.ekstern-varsling-status.topic
     */
    @Test
    fun `konsumerer innhold på topic fra dev`() = runTestApplicationWithDatabase {
        val processor = VarslingStatusRecordProcessor(VarslingStatusRepository(database))
        val sampleTopic = requireNotNull(javaClass.getResource("/fager.ekstern-varsling-status.topic")) {
            "Resource fager.ekstern-varsling-status.topic not found"
        }

        sampleTopic.readText().lines().forEach { line ->
            if (line.isNotBlank()) {
                processor.processRecordValue(line)
            }
        }
    }
}