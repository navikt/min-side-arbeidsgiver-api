package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.provideApplicationObjectMapper
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.digisyfo.VarslingStatusRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class VarslingStatusRepositoryTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<VarslingStatusRepository>(VarslingStatusRepository::class)
                provide<KontaktInfoPollerRepository>(KontaktInfoPollerRepository::class)
                provideApplicationObjectMapper()
                provide<VarslingStatusService>(VarslingStatusService::class)
                provide<AltinnService> { Mockito.mock<AltinnService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `henter virksomheter som har MANGLER_KOFUVI som nyeste status`() = app.runTest {
        listOf(
            // 42 siste status = OK
            Triple("OK", "2021-01-03T00:00:00Z", "42"),
            Triple("MANGLER_KOFUVI", "2021-01-02T00:00:00Z", "42"),

            // 314 siste status = MANGLER_KOFUVI
            Triple("MANGLER_KOFUVI", "2021-01-03T00:00:00Z", "314"),
            Triple("OK", "2021-01-02T00:00:00Z", "314"),

            // 333 siste status = MANGLER_KOFUVI, men har kontaktinfo fra poll
            Triple("MANGLER_KOFUVI", "2021-01-03T00:00:00Z", "333"),
            Triple("OK", "2021-01-02T00:00:00Z", "333"),
        ).forEachIndexed { index, (status, timestamp, vnr) ->
            app.processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "$vnr",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$timestamp",
                        "status": "$status",
                        "version": "1"
                    }
                """
            )
        }
        app.getDependency<KontaktInfoPollerRepository>().updateKontaktInfo("333", true, true)

        val result = app.getDependency<VarslingStatusRepository>().hentVirksomheterMedFeil(10000.days)
        assertEquals(listOf("314"), result)
    }

    @Test
    fun `sletter statuser eldre enn`() = app.runTest {
        val now = Instant.now()
        listOf(
            now.minusSeconds(1) to "1000",
            now.minusSeconds(2) to "2000",
            now.minusSeconds(3) to "3000",
            now.minusSeconds(4) to "4000",
            now.minusSeconds(5) to "5000",
            now.minusSeconds(6) to "6000",
        ).forEachIndexed { index, (timestamp, vnr) ->
            app.processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "$vnr",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$timestamp",
                        "status": "MANGLER_KOFUVI",
                        "version": "1"
                    }
                """
            )
        }
        app.getDependency<VarslingStatusRepository>().slettVarslingStatuserEldreEnn(3.seconds)

        val result = app.getDependency<VarslingStatusRepository>().hentVirksomheterMedFeil(1.days)
        assertEquals(listOf("1000", "2000").sorted(), result.sorted())
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