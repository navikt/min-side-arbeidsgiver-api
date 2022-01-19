package no.nav.arbeidsgiver.min_side.services.digisyfo;

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

    private final JdbcTemplate jdbcTemplate;

    public NærmestelederRepositoryImpl(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean erNærmesteLederForNoen(String lederFnr) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from naermeste_leder where naermeste_leder_fnr = ?",
                Long.class,
                lederFnr
        );
        return count != null && count > 0;
    }

    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "digisyfoKafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, NarmesteLederHendelse> record) {
        log.info(
                "prosesserer kafka hendelse offset={} partition={} topic={}",
                record.offset(), record.partition(), record.topic()
        );

        // TODO: metric på retries
        NarmesteLederHendelse hendelse = record.value();
        if (hendelse.aktivTom != null) {
            jdbcTemplate.update("delete from naermeste_leder where id = ?", hendelse.narmesteLederId);
        } else {
            jdbcTemplate.update(
                    "insert into naermeste_leder(id, naermeste_leder_fnr)" +
                            "  values(?, ?)" +
                            "  on conflict (id) do nothing;",
                    hendelse.narmesteLederId,
                    hendelse.narmesteLederFnr
            );
        }
    }
}
