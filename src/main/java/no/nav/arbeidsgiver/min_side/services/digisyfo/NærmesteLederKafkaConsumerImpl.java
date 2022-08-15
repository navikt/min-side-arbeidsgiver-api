package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev-gcp", "prod-gcp"})
public class NærmesteLederKafkaConsumerImpl {
    private final ObjectMapper objectMapper;
    private final NærmestelederRepository nærmestelederRepository;

    public NærmesteLederKafkaConsumerImpl(
            ObjectMapper objectMapper,
            NærmestelederRepository nærmestelederRepository
    ) {
        this.objectMapper = objectMapper;
        this.nærmestelederRepository = nærmestelederRepository;
    }

    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-2", brukt 23.05.2022
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-3", brukt 16.06.2022
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        NarmesteLederHendelse hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse.class);
        nærmestelederRepository.processEvent(hendelse);
    }
}
