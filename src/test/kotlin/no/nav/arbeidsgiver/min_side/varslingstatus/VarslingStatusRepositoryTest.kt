package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.infrastruktur.database
import no.nav.arbeidsgiver.min_side.infrastruktur.processRecordValue
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplicationWithDatabase
import no.nav.arbeidsgiver.min_side.services.digisyfo.VarslingStatusRecordProcessor
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class VarslingStatusRepositoryTest {

    @Test
    fun `henter virksomheter som har MANGLER_KOFUVI som nyeste status`() = runTestApplicationWithDatabase {
        val kontaktInfoPollerRepository = KontaktInfoPollerRepository(database)
        val varslingStatusRepository = VarslingStatusRepository(database)
        val processor = VarslingStatusRecordProcessor(varslingStatusRepository)

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
            processor.processRecordValue(
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
        kontaktInfoPollerRepository.updateKontaktInfo("333", harEpost = true, harTlf = true)

        val result = varslingStatusRepository.hentVirksomheterMedFeil(10000.days)
        assertEquals(listOf("314"), result)
    }

    @Test
    fun `sletter statuser eldre enn`() = runTestApplicationWithDatabase {
        val varslingStatusRepository = VarslingStatusRepository(database)
        val processor = VarslingStatusRecordProcessor(varslingStatusRepository)
        val now = Instant.now()

        listOf(
            now.minusSeconds(1) to "1000",
            now.minusSeconds(2) to "2000",
            now.minusSeconds(3) to "3000",
            now.minusSeconds(4) to "4000",
            now.minusSeconds(5) to "5000",
            now.minusSeconds(6) to "6000",
        ).forEachIndexed { index, (timestamp, vnr) ->
            processor.processRecordValue(
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
        varslingStatusRepository.slettVarslingStatuserEldreEnn(3.seconds)

        val result = varslingStatusRepository.hentVirksomheterMedFeil(1.days)
        assertEquals(listOf("1000", "2000").sorted(), result.sorted())
    }
}