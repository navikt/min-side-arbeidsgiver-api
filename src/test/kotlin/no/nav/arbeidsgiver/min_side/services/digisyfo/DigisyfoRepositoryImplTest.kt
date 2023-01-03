package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import org.assertj.core.api.Assertions
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.*

class DigisyfoRepositoryImplTest {
    private val leder1 = "10011223344"
    private val leder2 = "20011223344"
    private val ansatt1 = "10044332211"
    private val ansatt2 = "20044332211"
    private val ansatt3 = "30044332211"
    private val vnr1 = "100111222"
    private val vnr2 = "200111222"
    private val vnr3 = "300111222"
    private val uuid1 = "3608d78e-10a3-4179-9cac-000000000001"
    private val uuid2 = "3608d78e-10a3-4179-9cac-000000000002"
    private val uuid3 = "3608d78e-10a3-4179-9cac-000000000003"

    @Test
    fun `sletter ikke dagens sykmeldinger`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(NL(uuid1, leder1, ansatt1, vnr1)),
            sykmeldinger = listOf(SM("1", EV(ansatt1, vnr1, listOf("2020-01-01")))),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `sletter gamle sykmeldinger`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-05-02",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt2, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("2", EV(ansatt2, vnr2, listOf("2020-01-02"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr2 to 0))
    }

    @Test
    fun `upsert bruker siste versjon`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }

    @Test
    fun `tombstones fjerner sykmeldinger`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf())
    }

    @Test
    fun `bruker eldste tom-dato`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-05-02",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01", "2020-05-02"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `tilgang selv uten aktiv sykmeldt`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2022-06-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt2, vnr2),
                NL(uuid3, leder1, ansatt3, vnr3),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2022-01-01"))),
                SM("2", EV(ansatt2, vnr2, listOf("2022-03-01"))),
                SM("3", EV(ansatt3, vnr3, listOf("2022-07-01"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr2 to 0, vnr3 to 1))
    }

    @Test
    fun `ser ikke ansatt som er sykmeldt i annen bedrift`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder2, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf())
        Assertions.assertThat(lookup(leder2)).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }

    @Test
    fun `ser ikke ansatt i samme bedrift man ikke er leder for`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder2, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf())
        Assertions.assertThat(lookup(leder2)).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }

    @Test
    fun `finner kun ansatt med aktiv sykmelding`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2022-11-07",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt2, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2022-11-01"))),
                SM("2", EV(ansatt2, vnr1, listOf("2022-11-21"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `to aktive sykmeldinger på en person gir en sykmeldt`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2022-11-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2022-11-01"))),
                SM("2", EV(ansatt1, vnr1, listOf("2022-11-21"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `aktive sykmeldinger på forskjellige person holdes seperat`() {
        val lookup = prepareDatabaseSingletonBatches(
            today = "2022-11-01",
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

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr1 to 2))
    }

    @Test
    fun `batch upsert – tombstone`() {
        val lookup = prepareDatabaseBatched(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf())
    }

    @Test
    fun `batch upsert – upsert`() {
        val lookup = prepareDatabaseBatched(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }

    @Test
    fun `batch tombstone – upsert`() {
        val lookup = prepareDatabaseBatched(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", null),
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
            ),
        )

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr1 to 1))
    }

    @Test
    fun `batch upsert – tombstone – upsert`() {
        val lookup = prepareDatabaseBatched(
            today = "2020-01-01",
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

        Assertions.assertThat(lookup(leder1)).containsExactlyEntriesOf(mapOf(vnr2 to 1))
    }




    private data class NL(val id: String, val fnrLeder: String, val fnrAnsatt: String, val vnr: String)
    private data class SM(val key: String, val event: EV?)
    private data class EV(val fnr: String, val vnr: String, val dates: List<String>)

    private fun prepareDatabaseSingletonBatches(today: String, nærmasteLedere: List<NL>, sykmeldinger: List<SM>) =
        prepareDatabase(
            nærmasteLedere,
            sykmeldinger.map {
                if (it.event == null) {
                    listOf(it.key to null)
                } else {
                    listOf(it.key to SykmeldingHendelse.create(it.event.fnr, it.event.vnr, it.event.dates))
                }
            },
            today
        )

    private fun prepareDatabaseBatched(today: String, nærmasteLedere: List<NL>, sykmeldinger: List<SM>) =
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
            today
        )

    private fun prepareDatabase(nærmesteLedere: List<NL>, batches: List<List<Pair<String, SykmeldingHendelse?>>>, today: String) : (String) -> Map<String, Int> {
        val ds = PGSimpleDataSource()
        ds.setUrl("jdbc:postgresql://localhost:2345/postgres?user=postgres&password=postgres")

        val flyway = Flyway.configure()
            .dataSource(ds)
            .cleanDisabled(false)
            .load()
        flyway.clean()
        flyway.migrate()
        val jdbcTemplate = JdbcTemplate(ds)

        val digisyfoRepository = DigisyfoRepositoryImpl(jdbcTemplate, LoggingMeterRegistry())

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

        digisyfoRepository.deleteOldSykmelding(LocalDate.parse(today))



        return { fnr: String ->
            digisyfoRepository.virksomheterOgSykmeldte(fnr, LocalDate.parse(today))
                .associate { it.virksomhetsnummer to it.antallSykmeldte }
        }
    }
}
