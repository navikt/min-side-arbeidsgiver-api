package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.*

class SykmeldingRepositoryImplTest {
    private val leder1 = "10011223344"
    private val leder2 = "20011223344"
    private val ansatt1 = "10044332211"
    private val ansatt2 = "20044332211"
    private val vnr1 = "100111222"
    private val vnr2 = "200111222"
    private val uuid1 = "3608d78e-10a3-4179-9cac-000000000001"
    private val uuid2 = "3608d78e-10a3-4179-9cac-000000000002"

    @Test
    fun `sletter ikke dagens sykmeldinger`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(NL(uuid1, leder1, ansatt1, vnr1)),
            sykmeldinger = listOf(SM("1", EV(ansatt1, vnr1, listOf("2020-01-01")))),
            deleteFrom = LocalDate.parse("2020-01-01"),
        )

        val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
        assertThat(result).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `sletter gamle sykmeldinger`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt2, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("2", EV(ansatt2, vnr2, listOf("2020-01-02"))),
            ),
            deleteFrom = LocalDate.parse("2020-05-02"),
        )

        val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
        assertThat(result).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }

    @Test
    fun `upsert bruker siste versjon`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
        assertThat(result).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }

    @Test
    fun `tombstones fjerner sykmeldinger`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
            ),
        )

        val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
        assertThat(result).containsExactlyEntriesOf(mapOf())
    }

    @Test
    fun `bruker eldste tom-dato`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01", "2020-05-02"))),
            ),
            deleteFrom = LocalDate.parse("2020-05-02"),
        )

        val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
        assertThat(result).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `ser ikke ansatt som er sykmeldt i annen bedrift`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder2, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf())
        }

        sykmeldingRepository.oversiktSykmeldinger(leder2).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr2 to 1))
        }
    }

    @Test
    fun `ser ikke ansatt i samme bedrift man ikke er leder for`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder2, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf())
        }

        sykmeldingRepository.oversiktSykmeldinger(leder2).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr2 to 1))
        }
    }

    @Test
    fun `to aktive sykmeldinger på en person gir en sykmeldt`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2022-11-01"))),
                SM("2", EV(ansatt1, vnr1, listOf("2022-11-21"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr1 to 2))
        }
    }

    @Test
    fun `aktive sykmeldinger på forskjellige person holdes seperat`() {
        val sykmeldingRepository = prepareDatabaseSingletonBatches(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt2, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2022-11-01"))),
                SM("2", EV(ansatt1, vnr1, listOf("2022-11-21"))),
                SM("3", EV(ansatt2, vnr1, listOf("2022-11-01"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr1 to 3))
        }
    }

    @Test
    fun `batch upsert – tombstone`() {
        val sykmeldingRepository = prepareDatabaseBatched(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf())
        }
    }

    @Test
    fun `batch upsert – upsert`() {
        val sykmeldingRepository = prepareDatabaseBatched(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr2 to 1))
        }
    }

    @Test
    fun `batch tombstone – upsert`() {
        val sykmeldingRepository = prepareDatabaseBatched(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", null),
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr1 to 1))
        }
    }

    @Test
    fun `batch upsert – tombstone – upsert`() {
        val sykmeldingRepository = prepareDatabaseBatched(
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        sykmeldingRepository.oversiktSykmeldinger(leder1).also {
            assertThat(it).containsExactlyEntriesOf(mapOf(vnr2 to 1))
        }
    }



    private data class NL(val id: String, val fnrLeder: String, val fnrAnsatt: String, val vnr: String)
    private data class SM(val key: String, val event: EV?)
    private data class EV(val fnr: String, val vnr: String, val dates: List<String>)

    private fun prepareDatabaseSingletonBatches(nærmasteLedere: List<NL>, sykmeldinger: List<SM>, deleteFrom: LocalDate? = null) =
        prepareDatabase(
            nærmasteLedere,
            sykmeldinger.map {
                if (it.event == null) {
                    listOf(it.key to null)
                } else {
                    listOf(it.key to SykmeldingHendelse.create(it.event.fnr, it.event.vnr, it.event.dates))
                }
            },
            deleteFrom
        )

    private fun prepareDatabaseBatched(nærmasteLedere: List<NL>, sykmeldinger: List<SM>) =
        prepareDatabase(
            nærmasteLedere,
            listOf(
                sykmeldinger.map {
                    if (it.event == null) {
                        it.key to null
                    } else {
                        it.key to SykmeldingHendelse.create(it.event.fnr, it.event.vnr, it.event.dates)
                    }
                }),
            null
        )

    private fun prepareDatabase(nærmesteLedere: List<NL>, batches: List<List<Pair<String, SykmeldingHendelse?>>>, deleteFrom: LocalDate?) : SykmeldingRepositoryImpl {
        val ds = PGSimpleDataSource()
        ds.setUrl("jdbc:postgresql://localhost:2345/postgres?user=postgres&password=postgres")

        val flyway = Flyway.configure()
            .dataSource(ds)
            .load()
        flyway.clean()
        flyway.migrate()
        val jdbcTemplate = JdbcTemplate(ds)

        val digisyfoRepository = DigisyfoRepositoryImpl(jdbcTemplate, LoggingMeterRegistry())
        val sykmeldingRepository = SykmeldingRepositoryImpl(jdbcTemplate)

        nærmesteLedere.forEach {
            digisyfoRepository.processNærmesteLederEvent(
                NarmesteLederHendelse(
                    UUID.fromString(it.id),
                    it.fnrLeder,
                    null,
                    it.vnr,
                    it.fnrAnsatt
                )
            )
        }
        batches.forEach {
            digisyfoRepository.processSykmeldingEvent(it)
        }

        if (deleteFrom != null) {
            digisyfoRepository.deleteOldSykmelding(deleteFrom)
        }

        return sykmeldingRepository
    }
}
