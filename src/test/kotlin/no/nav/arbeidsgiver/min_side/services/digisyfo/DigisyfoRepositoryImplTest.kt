package no.nav.arbeidsgiver.min_side.services.digisyfo

import no.nav.arbeidsgiver.min_side.infrastruktur.TestDatabase
import no.nav.arbeidsgiver.min_side.infrastruktur.testApplicationWithDatabase
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `sletter ikke dagens sykmeldinger`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(NL(uuid1, leder1, ansatt1, vnr1)),
            sykmeldinger = listOf(SM("1", EV(ansatt1, vnr1, listOf("2020-01-01")))),
        )

        assertEquals(
            mapOf(vnr1 to 1),
            lookup(leder1)
        )
    }

    @Test
    fun `sletter gamle sykmeldinger`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
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

        assertEquals(
            mapOf(vnr1 to 0, vnr2 to 0),
            lookup(leder1)
        )
    }

    @Test
    fun `upsert bruker siste versjon`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
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

        assertEquals(
            mapOf(vnr1 to 0, vnr2 to 1),
            lookup(leder1)
        )
    }

    @Test
    fun `tombstones fjerner sykmeldinger`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 0),
            lookup(leder1)
        )
    }

    @Test
    fun `bruker eldste tom-dato`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2020-05-02",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01", "2020-05-02"))),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 1),
            lookup(leder1)
        )
    }

    @Test
    fun `tilgang selv uten aktiv sykmeldt`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
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

        assertEquals(
            mapOf(vnr1 to 0, vnr2 to 0, vnr3 to 1),
            lookup(leder1)
        )
    }

    @Test
    fun `tilgang som nærmeste leder uten sykmeldte`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2022-06-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(),
        )

        assertEquals(
            mapOf(vnr1 to 0),
            lookup(leder1)
        )
    }

    @Test
    fun `ser ikke ansatt som er sykmeldt i annen bedrift`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder2, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 0),
            lookup(leder1)
        )
        assertEquals(
            mapOf(vnr2 to 1),
            lookup(leder2)
        )
    }

    @Test
    fun `ser ikke ansatt i samme bedrift man ikke er leder for`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder2, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 0),
            lookup(leder1)
        )
        assertEquals(
            mapOf(vnr2 to 1),
            lookup(leder2)
        )
    }

    @Test
    fun `finner kun ansatt med aktiv sykmelding`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
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

        assertEquals(
            mapOf(vnr1 to 1),
            lookup(leder1)
        )
    }

    @Test
    fun `to aktive sykmeldinger på en person gir en sykmeldt`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
            today = "2022-11-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2022-11-01"))),
                SM("2", EV(ansatt1, vnr1, listOf("2022-11-21"))),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 1),
            lookup(leder1)
        )
    }

    @Test
    fun `aktive sykmeldinger på forskjellige person holdes seperat`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareSingletonBatches(
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

        assertEquals(
            mapOf(vnr1 to 2),
            lookup(leder1)
        )
    }

    @Test
    fun `batch upsert – tombstone`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareBatched(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                SM("1", null),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 0),
            lookup(leder1)
        )
    }

    @Test
    fun `batch upsert – upsert`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareBatched(
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

        assertEquals(
            mapOf(vnr1 to 0, vnr2 to 1),
            lookup(leder1)
        )
    }


    @Test
    fun `batch tombstone – upsert`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareBatched(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
            ),
            sykmeldinger = listOf(
                SM("1", null),
                SM(
                    "1",
                    EV(ansatt1, vnr1, listOf("2020-01-01"))
                ),
            ),
        )

        assertEquals(
            mapOf(vnr1 to 1),
            lookup(leder1)
        )
    }


    @Test
    fun `batch upsert – tombstone – upsert`() = testApplicationWithDatabase { db ->
        val lookup = db.prepareBatched(
            today = "2020-01-01",
            nærmasteLedere = listOf(
                NL(uuid1, leder1, ansatt1, vnr1),
                NL(uuid2, leder1, ansatt1, vnr2),
            ),
            sykmeldinger = listOf(
                SM(
                    "1",
                    EV(ansatt1, vnr1, listOf("2020-01-01"))
                ),
                SM("1", null),
                SM(
                    "1",
                    EV(ansatt1, vnr2, listOf("2020-01-01"))
                ),
            ),
        )
        assertEquals(
            mapOf(
                vnr1 to 0,
                vnr2 to 1
            ),
            lookup(leder1)
        )
    }


    private data class NL(
        val id: String,
        val fnrLeder: String,
        val fnrAnsatt: String,
        val vnr: String
    )

    private data class SM(val key: String, val event: EV?)
    private data class EV(
        val fnr: String,
        val vnr: String,
        val dates: List<String>
    )

    private suspend fun TestDatabase.prepareSingletonBatches(
        today: String,
        nærmasteLedere: List<NL>,
        sykmeldinger: List<SM>
    ) =
        prepare(
            nærmasteLedere,
            sykmeldinger.map {
                if (it.event == null) {
                    listOf(it.key to null)
                } else {
                    listOf(
                        it.key to SykmeldingHendelse.create(
                            it.event.fnr,
                            it.event.vnr,
                            it.event.dates
                        )
                    )
                }
            },
            today
        )

    private suspend fun TestDatabase.prepareBatched(
        today: String,
        nærmasteLedere: List<NL>,
        sykmeldinger: List<SM>
    ) =
        prepare(
            nærmasteLedere,
            listOf(
                sykmeldinger.map {
                    if (it.event == null) {
                        it.key to null
                    } else {
                        it.key to SykmeldingHendelse.create(
                            it.event.fnr,
                            it.event.vnr,
                            it.event.dates
                        )
                    }
                }),
            today
        )

    private suspend fun TestDatabase.prepare(
        nærmesteLedere: List<NL>,
        batches: List<List<Pair<String, SykmeldingHendelse?>>>,
        today: String
    ): suspend (String) -> Map<String, Int> {
        val digisyfoRepository = DigisyfoRepositoryImpl(this)

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

        digisyfoRepository.deleteOldSykmelding(
            LocalDate.parse(
                today
            )
        )

        return { fnr: String ->
            digisyfoRepository.virksomheterOgSykmeldte(
                fnr,
                LocalDate.parse(today)
            ).associate { it.virksomhetsnummer to it.antallSykmeldte }
        }
    }
}
