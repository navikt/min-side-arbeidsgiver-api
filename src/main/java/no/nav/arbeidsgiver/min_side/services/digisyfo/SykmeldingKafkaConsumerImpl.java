package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile({"dev-gcp", "prod-gcp"})
@Slf4j
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
            containerFactory = "digisyfoSykmeldingKafkaListenerContainerFactory",
            properties = ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=2"
    )
    public void processConsumerRecord(
            List<ConsumerRecord<String, String>> records
    ) throws JsonProcessingException {
        var msg = records
                .stream()
                .map(it -> String.format("p:%d o:%d", it.partition(), it.offset()))
                .collect(Collectors.joining("; "));
        log.info("batch info size: {}, records: {}", records.size(), msg);
         throw new RuntimeException("simulate failure");
//        records.stream().map(r ->
//
//        );
//        var hendelse = record.value() == null
//                ? null
//                : objectMapper.readValue(record.value(), SykmeldingHendelse.class);
//        sykmeldingRepository.processEvent(record.key(), hendelse);
    }
}
