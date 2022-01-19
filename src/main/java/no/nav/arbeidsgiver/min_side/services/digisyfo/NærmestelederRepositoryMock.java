package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Repository;

@Profile({"labs"})
@Slf4j
@Repository
public class NærmestelederRepositoryMock implements NærmestelederRepository {
    @Override
    public boolean erNærmesteLederForNoen(String lederFnr) {
        return true;
    }
}
