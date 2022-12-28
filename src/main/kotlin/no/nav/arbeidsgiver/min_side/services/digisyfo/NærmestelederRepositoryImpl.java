package no.nav.arbeidsgiver.min_side.services.digisyfo;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"dev-gcp", "prod-gcp"})
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

}
