package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collector
import java.util.stream.Collectors

interface DigisyfoRepository {
    data class Virksomhetsinfo(
        val virksomhetsnummer: String = "",
        val antallSykmeldte: Int = 0
    )

    fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<Virksomhetsinfo>
    fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse)
    fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>)
}

@Profile("dev-gcp", "prod-gcp")
@Repository
class DigisyfoRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    meterRegistry: MeterRegistry,
) : DigisyfoRepository {
    private val tombstoneCounter: Counter
    private val expiredCounter: Counter
    private val updateCounter: Counter

    init {
        tombstoneCounter = Counter.builder("fager.msa.sykmelding.tombstones")
            .description("antall sykmelding-tombstones prosessert")
            .register(meterRegistry)
        expiredCounter = Counter.builder("fager.msa.sykmelding.expired")
            .description("antall sykmeldinger som er slettet fordi de er utløpt")
            .register(meterRegistry)
        updateCounter = Counter.builder("fager.msa.sykmelding.updated")
            .description("antall sykmeldinger som er opprettet/oppdatert")
            .register(meterRegistry)
    }

    override fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse) {
        if (hendelse.aktivTom != null) {
            jdbcTemplate.update("delete from naermeste_leder where id = ?") { ps: PreparedStatement ->
                ps.setObject(1, hendelse.narmesteLederId)
            }
        } else {
            jdbcTemplate.update(
                """
                insert into naermeste_leder(id, naermeste_leder_fnr, virksomhetsnummer, ansatt_fnr)  
                values(?, ?, ?, ?)  
                on conflict (id) 
                do nothing;
                """.trimIndent()
            ) { ps: PreparedStatement ->
                ps.setObject(1, hendelse.narmesteLederId)
                ps.setString(2, hendelse.narmesteLederFnr)
                ps.setString(3, hendelse.virksomhetsnummer)
                ps.setString(4, hendelse.ansattFnr)
            }
        }
    }

    override fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>) {
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
        jdbcTemplate.batchUpdate("delete from sykmelding where id = ?", tombstones)
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
        jdbcTemplate.batchUpdate(
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

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.MINUTES)
    fun deleteOldSykmelding() {
        deleteOldSykmelding(LocalDate.now(ZoneId.of("Europe/Oslo")))
    }

    fun deleteOldSykmelding(today: LocalDate) {
        val rowsAffected: Int = jdbcTemplate.update(
            """
            delete from sykmelding where sykmeldingsperiode_slutt < ?
            """,
            today.minusMonths(4)
        )
        expiredCounter.increment(rowsAffected.toDouble())
    }

    override fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<DigisyfoRepository.Virksomhetsinfo> {
        return virksomheterOgSykmeldte(
            nærmestelederFnr, LocalDate.now(
                ZoneId.of("Europe/Oslo")
            )
        )
    }

    fun virksomheterOgSykmeldte(nærmestelederFnr: String?, sykmeldingSlutt: LocalDate?): List<DigisyfoRepository.Virksomhetsinfo> {
        jdbcTemplate.queryForStream(
            """
            with
            nl_koblinger as (
                select virksomhetsnummer, ansatt_fnr
                from naermeste_leder
                where naermeste_leder_fnr = ?
            ),
            virksomheter as (
                select distinct s.virksomhetsnummer as virksomhetsnummer
                from sykmelding as s
                join nl_koblinger nl on
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
            { ps: PreparedStatement ->
                ps.setString(1, nærmestelederFnr)
                ps.setDate(2, Date.valueOf(sykmeldingSlutt))
            },
            { rs: ResultSet, _: Int ->
                DigisyfoRepository.Virksomhetsinfo(
                    rs.getString("virksomhetsnummer"),
                    rs.getInt("antall_sykmeldte")
                )
            }
        ).use { stream -> return stream.toList() }
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

@Profile("local")
@Repository
class DigisyfoRepositoryStub : DigisyfoRepository {
    override fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<DigisyfoRepository.Virksomhetsinfo> {
        return listOf(DigisyfoRepository.Virksomhetsinfo("910825526", 4))
    }

    override fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse) {}
    override fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>) {}
}