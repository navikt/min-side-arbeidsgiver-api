package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"dev-gcp", "prod-gcp"})
@Slf4j
@Repository
public class NærmestelederRepositoryImpl implements NærmestelederRepository {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public NærmestelederRepositoryImpl(
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.objectMapper = objectMapper;
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

    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-2", brukt 23.05.2022
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "digisyfoKafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        NarmesteLederHendelse hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse.class);
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

    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-3",
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-2", brukt 23.05.2022
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "digisyfoKafkaListenerContainerFactory"
    )
    public void updateOldRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        NarmesteLederHendelse hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse.class);
        jdbcTemplate.update(
                "update naermeste_leder set ansatt_fnr = ? where id = ?",
                ps -> {
                    ps.setString(1, hendelse.ansattFnr);
                    ps.setObject(2, hendelse.narmesteLederId);
                }
        );
    }
}
