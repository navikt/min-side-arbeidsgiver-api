package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.Nullable;
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
            topics = "teamsykmelding.syfo-sendt-sykmelding",
            containerFactory = "digisyfoSykmeldingKafkaListenerContainerFactory",
            properties = ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=2"
    )
    public void processConsumerRecord(
            List<ConsumerRecord<String, String>> records
    ) {
        log.info("processConsumerRecord entered with {} records", records.size());
        try {
            var parsedRecords = records
                    .stream()
                    .map(r ->
                            ImmutablePair.of(
                                    r.key(),
                                    getSykmeldingHendelse(r)
                            )
                    ).collect(Collectors.toList());
            sykmeldingRepository.processEvent(parsedRecords);
        } catch (Exception e) {
            log.error("exception while parsing/processing messages {}", e.getMessage());
            throw e;
        }
        log.info("processConsumerRecord returns");
    }

    @Nullable
    private SykmeldingHendelse getSykmeldingHendelse(ConsumerRecord<String, String> r) {
        try {
            return r.value() == null
                    ? null
                    : objectMapper.readValue(r.value(), SykmeldingHendelse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
