package no.nav.arbeidsgiver.min_side.services.digisyfo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile({"dev-gcp", "prod-gcp"})
@Repository
@Slf4j
public class DigisyfoRepositoryImpl implements DigisyfoRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Counter tombstoneCounter;
    private final Counter expiredCounter;
    private final Counter updateCounter;

    public DigisyfoRepositoryImpl(
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry
    ) {
        this.jdbcTemplate = jdbcTemplate;

        tombstoneCounter = Counter
                .builder("fager.msa.sykmelding.tombstones")
                .description("antall sykmelding-tombstones prosessert")
                .register(meterRegistry);
        expiredCounter = Counter
                .builder("fager.msa.sykmelding.expired")
                .description("antall sykmeldinger som er slettet fordi de er utløpt")
                .register(meterRegistry);
        updateCounter = Counter
                .builder("fager.msa.sykmelding.updated")
                .description("antall sykmeldinger som er opprettet/oppdatert")
                .register(meterRegistry);
    }

    @Override
    public void processNærmesteLederEvent(NarmesteLederHendelse hendelse) {
        if (hendelse.aktivTom != null) {
            jdbcTemplate.update(
                    "delete from naermeste_leder where id = ?",
                    ps -> ps.setObject(1, hendelse.narmesteLederId)
            );
        } else {
            jdbcTemplate.update(
                    "insert into naermeste_leder(id, naermeste_leder_fnr, virksomhetsnummer, ansatt_fnr)" +
                            "  values(?, ?, ?, ?)" +
                            "  on conflict (id) do nothing;",
                    ps -> {
                        ps.setObject(1, hendelse.narmesteLederId);
                        ps.setString(2, hendelse.narmesteLederFnr);
                        ps.setString(3, hendelse.virksomhetsnummer);
                        ps.setString(4, hendelse.ansattFnr);
                    }
            );
        }
    }

    @Override
    public void processSykmeldingEvent(List<ImmutablePair<String, SykmeldingHendelse>> batchRecords) {
        var records = batchRecords.stream()
                .collect(latestBy(ImmutablePair::getKey));

        var tombstones = records
                .stream()
                .filter(it -> it.getValue() == null)
                .map(it -> new Object[] { it.getKey() })
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate("delete from sykmelding where id = ?",  tombstones);
        tombstoneCounter.increment(tombstones.size());

        var upserts = records.stream()
                .filter(it -> it.getValue() != null)
                .map(it -> new Object[] {
                                it.getKey(),
                                it.getValue().event.arbeidsgiver.virksomhetsnummer,
                                it.getValue().kafkaMetadata.fnrAnsatt,
                                it.getValue().sykmelding.sykmeldingsperioder
                                        .stream()
                                        .map((periode) -> periode.tom)
                                        .max(LocalDate::compareTo)
                                        .orElseThrow()
                        }
                )
                .collect(Collectors.toList());

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
        );
        updateCounter.increment(upserts.size());
    }

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.MINUTES)
    public void deleteOldSykmelding() {
        deleteOldSykmelding(LocalDate.now(ZoneId.of("Europe/Oslo")));
    }

    public void deleteOldSykmelding(LocalDate today) {
        int rowsAffected = jdbcTemplate.update(
                """
                    delete from sykmelding where sykmeldingsperiode_slutt < ?
                """,
                today.minusMonths(4)
        );
        expiredCounter.increment(rowsAffected);
    }

    /**
     * Deduplicate the stream, identifiying objects by keyMapper, and keeping
     * the latest version (further back in the stream). The order in the final
     * result may differ from order of the stream.
     */
    public static <T, K> Collector<T, ?, Collection<T>> latestBy(Function<? super T, ? extends K> keyMapper) {
        return Collectors.collectingAndThen(
                Collectors.toMap(
                        keyMapper,
                        Function.identity(),
                        (older, newer) -> newer
                ),
                Map::values);
    }

    @Override
    public List<Virksomhetsinfo> sykmeldtePrVirksomhet(String nærmestelederFnr) {
        return sykmeldtePrVirksomhet(nærmestelederFnr, LocalDate.now(
                ZoneId.of("Europe/Oslo")
        ));
    }
    public List<Virksomhetsinfo> sykmeldtePrVirksomhet(String nærmestelederFnr, LocalDate sykmeldingSlutt) {
        try (Stream<Virksomhetsinfo> stream = jdbcTemplate.queryForStream(
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
                (PreparedStatement ps) -> {
                    ps.setString(1, nærmestelederFnr);
                    ps.setDate(2, Date.valueOf(sykmeldingSlutt));
                },
                (ResultSet rs, int row) -> new Virksomhetsinfo(
                        rs.getString("virksomhetsnummer"),
                        rs.getInt("antall_sykmeldte")
                )
        )) {
            return stream.collect(Collectors.toList());
        }
    }
}
