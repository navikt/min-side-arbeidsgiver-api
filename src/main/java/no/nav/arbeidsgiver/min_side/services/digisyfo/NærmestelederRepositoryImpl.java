package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"dev-gcp", "prod-gcp"})
@Slf4j
@Repository
public class NærmestelederRepositoryImpl implements NærmestelederRepository {
    private final JdbcTemplate jdbcTemplate;

    public NærmestelederRepositoryImpl(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> virksomheterSomNærmesteLeder(String lederFnr) {
        return jdbcTemplate.queryForList(
                "select distinct virksomhetsnummer from naermeste_leder where naermeste_leder_fnr = ?",
                String.class,
                lederFnr
        );
    }

    @Override
    public void processEvent(NarmesteLederHendelse hendelse) {
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
}
