package no.nav.arbeidsgiver.min_side.services.digisyfo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Profile({"dev-gcp", "prod-gcp"})
@Repository
@Slf4j
public class SykmeldingRepositoryImpl implements InitializingBean, SykmeldingRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    SykmeldingRepositoryImpl(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    private Counter tombstoneCounter;
    private Counter expiredCounter;
    private Counter updateCounter;

    @Override
    public void afterPropertiesSet() {
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
                                    select virksomhetsnummer, fnr_ansatt from naermeste_leder where naermeste_leder_fnr = ?
                                )
                                select s.virksomhetsnummer as virksomhetsnummer, count(*) as antall
                                from sykmelding as s
                                join nl on 
                                nl.virksomhetsnummer = s.virksomhetsnummer and 
                                nl.fnr_ansatt = s.fnr_ansatt
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
    public void processEvent(String key, SykmeldingHendelse hendelse) {
        if (hendelse == null) {
            jdbcTemplate.update("delete from sykmelding where id = ?", key);
            tombstoneCounter.increment();
        } else {
            var tom = hendelse.sykmelding.sykmeldingsperioder
                    .stream()
                    .map((it) -> it.tom)
                    .max(LocalDate::compareTo)
                    .orElseThrow();
            jdbcTemplate.update(
                    """
                        insert into sykmelding
                        (id, virksomhetsnummer, fnr_ansatt, sykmeldingsperiode_slutt)
                        values (?, ?, ?, ?)
                        on conflict do update set
                        virksomhetsnummer = EXCLUDED.virksomhetsnummer,
                        fnr_ansatt= EXCLUDED.fnr_ansatt,
                        sykmeldingsperiode_slutt = EXCLUDED.sykmeldingsperiode_slutt
                        """,
                    key,
                    hendelse.event.arbeidsgiver.virksomhetsnummer,
                    hendelse.kafkaMetadata.fnrAnsatt,
                    tom
            );
            updateCounter.increment();
        }
    }

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.MINUTES)
    public void deleteOldSykmelding() {
        deleteOldSykmelding(
            LocalDate.now(ZoneId.of("Europe/Oslo")).minusMonths(4)
        );
    }

    public void deleteOldSykmelding(LocalDate cutoff) {
        int rowsAffected = jdbcTemplate.update(
                """
                    delete from sykmelding where sykmeldingsperiode_slutt < ?
                """,
                cutoff
        );
        expiredCounter.increment(rowsAffected);
    }
}
