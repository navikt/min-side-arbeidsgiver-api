package no.nav.arbeidsgiver.min_side.services.digisyfo

import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.infrastruktur.TestDatabase
import no.nav.arbeidsgiver.min_side.infrastruktur.testApplicationWithDatabase
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `sletter ikke dagens sykmeldinger`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
                nærmasteLedere = listOf(NL(uuid1, leder1, ansatt1, vnr1)),
                sykmeldinger = listOf(SM("1", EV(ansatt1, vnr1, listOf("2020-01-01")))),
                deleteFrom = LocalDate.parse("2020-01-01"),
            )

            val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
            assertEquals(
                mapOf(vnr1 to 1),
                result
            )
        }
    }

    @Test
    fun `sletter gamle sykmeldinger`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
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
            assertEquals(
                mapOf(vnr2 to 1),
                result
            )
        }
    }

    @Test
    fun `upsert bruker siste versjon`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
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
            assertEquals(
                mapOf(vnr2 to 1),
                result
            )
        }
    }

    @Test
    fun `tombstones fjerner sykmeldinger`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                ),
                sykmeldinger = listOf(
                    SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                    SM("1", null),
                ),
            )

            val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
            assertEquals(
                mapOf(),
                result
            )
        }
    }

    @Test
    fun `bruker eldste tom-dato`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                ),
                sykmeldinger = listOf(
                    SM("1", EV(ansatt1, vnr1, listOf("2020-01-01", "2020-05-02"))),
                ),
                deleteFrom = LocalDate.parse("2020-05-02"),
            )

            val result = sykmeldingRepository.oversiktSykmeldinger(leder1)
            assertEquals(
                mapOf(vnr1 to 1),
                result
            )
        }
    }


    @Test
    fun `ser ikke ansatt som er sykmeldt i annen bedrift`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                    NL(uuid2, leder2, ansatt1, vnr2),
                ),
                sykmeldinger = listOf(
                    SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
                ),
            )

            sykmeldingRepository.oversiktSykmeldinger(leder1).also {
                assertEquals(
                    mapOf(),
                    it
                )
            }

            sykmeldingRepository.oversiktSykmeldinger(leder2).also {
                assertEquals(
                    mapOf(vnr2 to 1),
                    it
                )
            }
        }
    }

    @Test
    fun `ser ikke ansatt i samme bedrift man ikke er leder for`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                    NL(uuid2, leder2, ansatt1, vnr2),
                ),
                sykmeldinger = listOf(
                    SM("1", EV(ansatt1, vnr2, listOf("2020-01-01"))),
                ),
            )

            sykmeldingRepository.oversiktSykmeldinger(leder1).also {
                assertEquals(
                    mapOf(),
                    it
                )
            }

            sykmeldingRepository.oversiktSykmeldinger(leder2).also {
                assertEquals(
                    mapOf(vnr2 to 1),
                    it
                )
            }
        }
    }

    @Test
    fun `to aktive sykmeldinger på en person gir en sykmeldt`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                ),
                sykmeldinger = listOf(
                    SM("1", EV(ansatt1, vnr1, listOf("2022-11-01"))),
                    SM("2", EV(ansatt1, vnr1, listOf("2022-11-21"))),
                ),
            )

            sykmeldingRepository.oversiktSykmeldinger(leder1).also {
                assertEquals(
                    mapOf(vnr1 to 2),
                    it
                )
            }
        }
    }

    @Test
    fun `aktive sykmeldinger på forskjellige person holdes seperat`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareSingletonBatches(
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
                assertEquals(
                    mapOf(vnr1 to 3),
                    it
                )
            }
        }
    }

    @Test
    fun `batch upsert – tombstone`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareBatched(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                ),
                sykmeldinger = listOf(
                    SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                    SM("1", null),
                ),
            )

            sykmeldingRepository.oversiktSykmeldinger(leder1).also {
                assertEquals(
                    mapOf(),
                    it
                )
            }
        }
    }

    @Test
    fun `batch upsert – upsert`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareBatched(
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
                assertEquals(
                    mapOf(vnr2 to 1),
                    it
                )
            }
        }
    }

    @Test
    fun `batch tombstone – upsert`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareBatched(
                nærmasteLedere = listOf(
                    NL(uuid1, leder1, ansatt1, vnr1),
                ),
                sykmeldinger = listOf(
                    SM("1", null),
                    SM("1", EV(ansatt1, vnr1, listOf("2020-01-01"))),
                ),
            )

            sykmeldingRepository.oversiktSykmeldinger(leder1).also {
                assertEquals(
                    mapOf(vnr1 to 1),
                    it
                )
            }
        }
    }

    @Test
    fun `batch upsert – tombstone – upsert`() = testApplicationWithDatabase { db ->
        runBlocking {
            val sykmeldingRepository = db.prepareBatched(
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
                assertEquals(
                    mapOf(vnr2 to 1),
                    it
                )
            }
        }
    }

    private data class NL(
        val id: String,
        val fnrLeder: String,
        val fnrAnsatt: String,
        val vnr: String
    )

    private data class SM(val key: String, val event: EV?)
    private data class EV(val fnr: String, val vnr: String, val dates: List<String>)

    private suspend fun TestDatabase.prepareSingletonBatches(
        nærmasteLedere: List<NL>,
        sykmeldinger: List<SM>,
        deleteFrom: LocalDate? = null
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
            deleteFrom
        )

    private suspend fun TestDatabase.prepareBatched(nærmasteLedere: List<NL>, sykmeldinger: List<SM>) =
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
            null
        )

    private suspend fun TestDatabase.prepare(
        nærmesteLedere: List<NL>,
        batches: List<List<Pair<String, SykmeldingHendelse?>>>,
        deleteFrom: LocalDate?
    ): SykmeldingRepositoryImpl {
        val digisyfoRepository = DigisyfoRepositoryImpl(this)
        val sykmeldingRepository = SykmeldingRepositoryImpl(this)

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
