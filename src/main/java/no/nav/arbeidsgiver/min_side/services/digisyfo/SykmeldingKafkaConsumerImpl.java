package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev-gcp", "prod-gcp"})
public class SykmeldingKafkaConsumerImpl {
    private final ObjectMapper objectMapper;
    private final SykmeldingRepository sykmeldingRepository;

    public SykmeldingKafkaConsumerImpl(ObjectMapper objectMapper, SykmeldingRepository sykmeldingRepository) {
        this.objectMapper = objectMapper;
        this.sykmeldingRepository = sykmeldingRepository;
    }

    @KafkaListener(
            id = "min-side-arbeidsgiver-sykmelding-1",
            topics = "teamsykmelding.teamsykmelding.syfo-sendt-sykmelding",
            containerFactory = "digisyfoKafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        var hendelse = record.value() == null
                ? null
                : objectMapper.readValue(record.value(), SykmeldingHendelse.class);
        sykmeldingRepository.processEvent(record.key(), hendelse);
    }
}
