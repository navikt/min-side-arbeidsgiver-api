package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev-gcp", "prod-gcp"})
public class NærmesteLederKafkaConsumerImpl {
    private final ObjectMapper objectMapper;
    private final NærmestelederRepository nærmestelederRepository;
    private final String cluster;

    public NærmesteLederKafkaConsumerImpl(
            ObjectMapper objectMapper,
            NærmestelederRepository nærmestelederRepository,
            @Value("${nais.cluster.name:local}") String cluster
    ) {
        this.objectMapper = objectMapper;
        this.nærmestelederRepository = nærmestelederRepository;
        this.cluster = cluster;
    }

    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-0",
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-2", brukt 23.05.2022
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-3", brukt 16.06.2022
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "digisyfoKafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, String> record) {
        try {
            if (cluster.equals("dev-gcp")) {
                throw new RuntimeException("simulert feil");
            }
            NarmesteLederHendelse hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse.class);
            nærmestelederRepository.processEvent(hendelse);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error(
                    "exception while processing kafka event exception={} topic={} parition={} offset={}",
                    e.getClass().getCanonicalName(),
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e
            );
            throw new RuntimeException(e);
        }
    }
}
