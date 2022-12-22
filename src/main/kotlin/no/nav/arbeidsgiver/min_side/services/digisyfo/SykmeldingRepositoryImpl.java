package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile({"dev-gcp", "prod-gcp"})
@Repository
@Slf4j
public class SykmeldingRepositoryImpl implements SykmeldingRepository {
    private final JdbcTemplate jdbcTemplate;

    SykmeldingRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr) {
        try (Stream<Pair<String, Integer>> stream = jdbcTemplate.queryForStream(
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
                (ResultSet rs, int row) -> new ImmutablePair<>(
                        rs.getString("virksomhetsnummer"),
                        rs.getInt("antall")
                )
        )) {
            return stream.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }
    }
}
