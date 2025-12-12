package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.infrastruktur.Database
import no.nav.arbeidsgiver.min_side.infrastruktur.processRecordValue
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplicationWithDatabase
import no.nav.arbeidsgiver.min_side.services.digisyfo.VarslingStatusRecordProcessor
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration


class KontaktInfoPollerRepositoryTest {
    @Test
    fun `sletter kontaktinfo med ok status eller eldre enn`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide(VarslingStatusRecordProcessor::class)
            provide(VarslingStatusRepository::class)
            provide(KontaktInfoPollerRepository::class)
        },
    ) {
        val database = resolve<Database>()
        val varslingStatusRecordProcessor = resolve<VarslingStatusRecordProcessor>()
        val kontaktInfoPollerRepository = resolve<KontaktInfoPollerRepository>()
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
            varslingStatusRecordProcessor.processRecordValue(
                """
                {
                        "virksomhetsnummer": "$vnr",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$tidspunkt",
                        "status": "$status",
                        "version": "1"
                    }
                """
            )
            kontaktInfoPollerRepository.updateKontaktInfo(vnr, harEpost = true, harTlf = true)
        }

        kontaktInfoPollerRepository.slettKontaktinfoMedOkStatusEllerEldreEnn(1.days)

        val kontaktinfo = database.nonTransactionalExecuteQuery(
            "select * from kontaktinfo_resultat",
            {},
            { it.getString("virksomhetsnummer") }
        )
        assertEquals(listOf("314"), kontaktinfo)
    }
}