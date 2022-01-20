package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Repository;

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
    public boolean erNærmesteLederForNoen(String lederFnr) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists(select * from naermeste_leder where naermeste_leder_fnr = ?)",
                Boolean.class,
                lederFnr
        );
        return Boolean.TRUE.equals(exists);
    }

    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "digisyfoKafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        log.info(
                "prosesserer kafka hendelse offset={} partition={} topic={}",
                record.offset(), record.partition(), record.topic()
        );
        NarmesteLederHendelse hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse.class);
        if (hendelse.aktivTom != null) {
            jdbcTemplate.update(
                    "delete from naermeste_leder where id = ?",
                    ps -> {
                        ps.setObject(1, hendelse.narmesteLederId);
                    }
            );
        } else {
            jdbcTemplate.update(
                    "insert into naermeste_leder(id, naermeste_leder_fnr)" +
                            "  values(?, ?)" +
                            "  on conflict (id) do nothing;",
                    ps -> {
                        ps.setObject(1, hendelse.narmesteLederId);
                        ps.setString(2, hendelse.narmesteLederFnr);
                    }
            );
        }
    }
}
