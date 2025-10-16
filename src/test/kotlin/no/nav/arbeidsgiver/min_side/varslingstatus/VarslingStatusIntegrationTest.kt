package no.nav.arbeidsgiver.min_side.varslingstatus

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
import no.nav.arbeidsgiver.min_side.services.digisyfo.VarslingStatusRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals

class VarslingStatusIntegrationTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<VarslingStatusService>(VarslingStatusService::class)
                provide<VarslingStatusRepository>(VarslingStatusRepository::class)
                provide<KontaktInfoPollerRepository>(KontaktInfoPollerRepository::class)
                provideDefaultObjectMapper()
                provide<AltinnService> { Mockito.mock<AltinnService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `bruker som ikke har tilgang får status ok som default`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harOrganisasjon("314", token)).thenReturn(false)

        app.processVarslingStatus(
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

        client.post("/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals("""{"status":"OK"}""", it.bodyAsText(), false)
        }
    }

    @Test
    fun `bruker med tilgang men ingen status i databasen får OK som default`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harOrganisasjon("314", token)).thenReturn(true)

        app.processVarslingStatus(
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

        client.post("/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals("""{"status":"OK"}""", it.bodyAsText(), false)
        }
    }

    @Test
    fun `returnerer siste status for virksomhet`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harOrganisasjon("314", token)).thenReturn(true)

        listOf(
            "MANGLER_KOFUVI" to "2021-01-02T00:00:00Z",
            "OK" to "2021-01-01T00:00:00Z",
            "MANGLER_KOFUVI" to "2021-01-04T00:00:00Z",
            "ANNEN_FEIL" to "2021-01-03T00:00:00Z",
        ).forEachIndexed { index, (status, timestamp) ->
            app.processVarslingStatus(
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
        client.post("/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
                """{
                    "status": "MANGLER_KOFUVI",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "kvittertEventTimestamp": "2021-01-04T00:00:00Z"
                    }""", it.bodyAsText(), true
            )
        }
    }


    @Test
    fun `returnerer siste status for virksomhet OK`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harOrganisasjon("314", token)).thenReturn(true)

        listOf(
            "MANGLER_KOFUVI" to "2021-01-01T00:00:00Z",
            "OK" to "2021-01-07T00:00:00Z",
            "ANNEN_FEIL" to "2021-01-02T00:00:00Z",
            "MANGLER_KOFUVI" to "2021-01-03T00:00:00Z",
        ).forEachIndexed { index, (status, timestamp) ->
            app.processVarslingStatus(
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

        client.post("/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
                """{
                    "status": "OK",
                    "varselTimestamp": "2021-01-01T00:00:00",
                    "kvittertEventTimestamp": "2021-01-07T00:00:00Z"
                    }""", it.bodyAsText(), true
            )
        }
    }

    @Test
    fun `får ok dersom kontaktinfo er pollet og funnet`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().harOrganisasjon("314", token)).thenReturn(true)

        app.processVarslingStatus(
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
        app.getDependency<KontaktInfoPollerRepository>()
            .updateKontaktInfo(virksomhetsnummer = "314", harEpost = true, harTlf = true)

        client.post("/api/varslingStatus/v1") {
            setBody("""{"virksomhetsnummer": "314"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
                """{"status": "OK"}""", it.bodyAsText(), false
            )
        }
    }

    /**
     * fil generert med:
     * kafka-console-consumer.sh --bootstrap-server $KAFKA_BROKERS --consumer.config $KAFKA_CONFIG/kafka.properties --topic fager.ekstern-varsling-status --formatter kafka.tools.DefaultMessageFormatter --property print.value=true --from-beginning --timeout-ms 30000 > fager.ekstern-varsling-status.topic
     */
    @Test
    fun `konsumerer innhold på topic fra dev`() = app.runTest {
        val sampleTopic = this::class.java.classLoader.getResource("fager.ekstern-varsling-status.topic")
            ?: throw IllegalArgumentException("Could not find resource file")

        sampleTopic.readText().lines().forEach { line ->
            if (line.isNotBlank()) {
                app.processVarslingStatus(line)
            }
        }
    }

    private suspend fun FakeApplication.processVarslingStatus(value: String) {
        val processor =
            VarslingStatusRecordProcessor(getDependency<ObjectMapper>(), getDependency<VarslingStatusRepository>())
        processor.processRecord(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
}