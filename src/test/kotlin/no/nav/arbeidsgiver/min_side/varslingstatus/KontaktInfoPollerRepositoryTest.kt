package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.Database
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.provideDefaultObjectMapper
import no.nav.arbeidsgiver.min_side.services.digisyfo.VarslingStatusRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration


class KontaktInfoPollerRepositoryTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<VarslingStatusRepository>(VarslingStatusRepository::class)
                provide<KontaktInfoPollerRepository>(KontaktInfoPollerRepository::class)
                provideDefaultObjectMapper()
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `sletter kontaktinfo med ok status eller eldre enn`() = app.runTest {
        listOf(
            // OK
            Triple("OK", "42", Instant.now().toString()),

            // OK and old
            Triple("OK", "43", Instant.now().minus(2.days.toJavaDuration()).toString()),

            // MANGLER_KOFUVI
            Triple("MANGLER_KOFUVI", "314", Instant.now().toString()),

            // MANGLER_KOFUVI and old
            Triple("MANGLER_KOFUVI", "315", Instant.now().minus(2.days.toJavaDuration()).toString()),
        ).forEachIndexed { index, (status, vnr, tidspunkt) ->
            app.processVarslingStatus(
                """
                {
                        "virksomhetsnummer": "$vnr",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$tidspunkt",
                        "status": "$status",
                        "version": "1"
                    }
                """.trimIndent()
            )
            app.getDependency<KontaktInfoPollerRepository>().updateKontaktInfo(vnr, harEpost = true, harTlf = true)
        }

        app.getDependency<KontaktInfoPollerRepository>().slettKontaktinfoMedOkStatusEllerEldreEnn(1.days)

        val kontaktinfo = app.getDependency<Database>().nonTransactionalExecuteQuery(
            "select * from kontaktinfo_resultat",
            {},
            { it.getString("virksomhetsnummer") }
        )
        assertEquals(listOf("314"), kontaktinfo)
    }


    private suspend fun FakeApplication.processVarslingStatus(value: String) {
        val processor = VarslingStatusRecordProcessor(
            getDependency<ObjectMapper>(),
            getDependency<VarslingStatusRepository>()
        )
        processor.processRecord(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
}