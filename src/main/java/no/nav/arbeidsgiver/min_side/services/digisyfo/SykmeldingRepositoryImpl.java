package no.nav.arbeidsgiver.min_side.services.digisyfo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Profile({"dev-gcp", "prod-gcp"})
@Repository
@Slf4j
public class SykmeldingRepositoryImpl implements SykmeldingRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Counter tombstoneCounter;
    private final Counter expiredCounter;
    private final Counter updateCounter;

    SykmeldingRepositoryImpl(
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
    public Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr) {
        return jdbcTemplate.queryForStream(
                        """
                                with nl as (
                                    select virksomhetsnummer, ansatt_fnr from naermeste_leder where naermeste_leder_fnr = ?
                                )
                                select s.virksomhetsnummer as virksomhetsnummer, count(*) as antall
                                from sykmelding as s
                                join nl on 
                                nl.virksomhetsnummer = s.virksomhetsnummer and 
                                nl.ansatt_fnr = s.ansatt_fnr
                                group by s.virksomhetsnummer
                            """,
                        (PreparedStatement ps) ->
                                ps.setString(1, nærmestelederFnr),
                        (RowMapper<Pair<String, Integer>>) (ResultSet rs, int row) -> new ImmutablePair<>(
                                rs.getString("virksomhetsnummer"),
                                rs.getInt("antall")
                        )
                )
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    public void processEvent(List<ImmutablePair<String, SykmeldingHendelse>> records) {
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
}
