package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.micrometer.core.instrument.Counter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import no.nav.arbeidsgiver.min_side.infrastruktur.Database
import no.nav.arbeidsgiver.min_side.infrastruktur.Metrics
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.function.Function
import java.util.stream.Collector
import java.util.stream.Collectors

interface DigisyfoRepository {
    data class Virksomhetsinfo(
        val virksomhetsnummer: String = "",
        val antallSykmeldte: Int = 0
    )

    suspend fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<Virksomhetsinfo>
    suspend fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse)
    suspend fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>)
    suspend fun deleteOldSykmelding(today: LocalDate)
}

class DigisyfoRepositoryImpl(
    private val database: Database,
) : DigisyfoRepository {
    private val tombstoneCounter: Counter = Counter.builder("fager.msa.sykmelding.tombstones")
        .description("antall sykmelding-tombstones prosessert")
        .register(Metrics.meterRegistry)
    private val expiredCounter: Counter = Counter.builder("fager.msa.sykmelding.expired")
        .description("antall sykmeldinger som er slettet fordi de er utløpt")
        .register(Metrics.meterRegistry)
    private val updateCounter: Counter = Counter.builder("fager.msa.sykmelding.updated")
        .description("antall sykmeldinger som er opprettet/oppdatert")
        .register(Metrics.meterRegistry)

    override suspend fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse) {
        if (hendelse.aktivTom != null) {
            database.nonTransactionalExecuteUpdate("delete from naermeste_leder where id = ?") {
                uuid(hendelse.narmesteLederId)
            }
        } else {
            database.nonTransactionalExecuteUpdate(
                """
                insert into naermeste_leder(id, naermeste_leder_fnr, virksomhetsnummer, ansatt_fnr)  
                values(?, ?, ?, ?)  
                on conflict (id) 
                do nothing;
                """.trimIndent()
            ) {
                uuid(hendelse.narmesteLederId)
                text(hendelse.narmesteLederFnr)
                text(hendelse.virksomhetsnummer)
                text(hendelse.ansattFnr)
            }
        }
    }

    override suspend fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>) {
        val deduplicated = records.stream().collect(latestBy(Pair<String?, SykmeldingHendelse?>::first))

        val tombstones = deduplicated
            .stream()
            .filter { (_, value): Pair<String?, SykmeldingHendelse?> -> value == null }
            .map { (key): Pair<String?, SykmeldingHendelse?> ->
                arrayOf<Any?>(
                    key
                )
            }
            .collect(Collectors.toList())
        database.batchUpdate("delete from sykmelding where id = ?", tombstones)
        tombstoneCounter.increment(tombstones.size.toDouble())
        val upserts = deduplicated
            .filter { (_, value): Pair<String?, SykmeldingHendelse?> -> value != null }
            .map { (key, value): Pair<String?, SykmeldingHendelse?> ->
                arrayOf<Any?>(
                    key,
                    value!!.event!!.arbeidsgiver!!.virksomhetsnummer,
                    value.kafkaMetadata!!.fnrAnsatt,
                    value.sykmelding!!.sykmeldingsperioder!!
                        .map { periode: SykmeldingHendelse.SykmeldingsperiodeAGDTO -> periode.tom }
                        .maxOfOrNull { it!! }
                )
            }
        database.batchUpdate(
            """
            insert into sykmelding
            (id, virksomhetsnummer, ansatt_fnr, sykmeldingsperiode_slutt)
            values (?, ?, ?, ?)
            on conflict (id) do update set
            virksomhetsnummer = EXCLUDED.virksomhetsnummer,
            ansatt_fnr = EXCLUDED.ansatt_fnr,
            sykmeldingsperiode_slutt = EXCLUDED.sykmeldingsperiode_slutt
            """,
            upserts
        )
        updateCounter.increment(upserts.size.toDouble())
    }

    override suspend fun deleteOldSykmelding(today: LocalDate) {
        val rowsAffected: Int = database.nonTransactionalExecuteUpdate(
            """
            delete from sykmelding where sykmeldingsperiode_slutt < ?
            """
        ) {
            date(today.minusMonths(4))
        }
        expiredCounter.increment(rowsAffected.toDouble())
    }

    override suspend fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<DigisyfoRepository.Virksomhetsinfo> {
        return virksomheterOgSykmeldte(
            nærmestelederFnr, LocalDate.now(
                ZoneId.of("Europe/Oslo")
            )
        )
    }

    suspend fun virksomheterOgSykmeldte(
        nærmestelederFnr: String,
        sykmeldingSlutt: LocalDate
    ): List<DigisyfoRepository.Virksomhetsinfo> {
        return database.nonTransactionalExecuteQuery(
            """
            with
            nl_koblinger as (
                select virksomhetsnummer, ansatt_fnr
                from naermeste_leder
                where naermeste_leder_fnr = ?
            ),
            virksomheter as (
                select distinct nl.virksomhetsnummer as virksomhetsnummer
                from nl_koblinger as nl
                left outer join sykmelding s on
                    nl.virksomhetsnummer = s.virksomhetsnummer and
                    nl.ansatt_fnr = s.ansatt_fnr
            ),
            sykmeldte as (
                select
                    s.virksomhetsnummer as virksomhetsnummer,
                    count(distinct(nl.ansatt_fnr)) as antall_sykmeldte
                from sykmelding as s
                join nl_koblinger nl on
                    nl.virksomhetsnummer = s.virksomhetsnummer and
                    nl.ansatt_fnr = s.ansatt_fnr
                where s.sykmeldingsperiode_slutt >= ?
                group by s.virksomhetsnummer
            )
            select
                v.virksomhetsnummer as virksomhetsnummer,
                coalesce(s.antall_sykmeldte, 0) as antall_sykmeldte
            from virksomheter v
            left join sykmeldte s using (virksomhetsnummer)
            """,
            {
                text(nærmestelederFnr)
                date(sykmeldingSlutt)
            },
            {
                DigisyfoRepository.Virksomhetsinfo(
                    it.getString("virksomhetsnummer"),
                    it.getInt("antall_sykmeldte")
                )
            }
        )
    }

    companion object {
        /**
         * Deduplicate the stream, identifiying objects by keyMapper, and keeping
         * the latest version (further back in the stream). The order in the final
         * result may differ from order of the stream.
         */
        fun <T, K> latestBy(keyMapper: Function<in T, out K>): Collector<T, *, Collection<T>>? {
            return Collectors.collectingAndThen(
                Collectors.toMap(
                    keyMapper,
                    Function.identity()
                ) { _: T, newer: T -> newer }
            ) { it.values }
        }
    }
}

suspend fun Application.startDeleteOldSykmeldingLoop(scope: CoroutineScope) {
    val digisyfoRepository = dependencies.resolve<DigisyfoRepository>()
    scope.launch {
        while (true) {
            digisyfoRepository.deleteOldSykmelding(LocalDate.now(ZoneId.of("Europe/Oslo")))
            delay(Duration.ofMinutes(3))
        }
    }
}