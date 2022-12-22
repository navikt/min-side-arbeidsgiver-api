package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile({"dev-gcp", "prod-gcp"})
public class DigisyfoKafkaConsumerImpl {
    private final DigisyfoRepository digisyfoRepository;
    private final ObjectMapper objectMapper;

    public DigisyfoKafkaConsumerImpl(
            ObjectMapper objectMapper,
            DigisyfoRepository digisyfoRepository
    ) {
        this.objectMapper = objectMapper;
        this.digisyfoRepository = digisyfoRepository;
    }

    @KafkaListener(
            id = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-2", brukt 23.05.2022
            // id = "min-side-arbeidsgiver-narmesteleder-model-builder-3", brukt 16.06.2022
            topics = "teamsykmelding.syfo-narmesteleder-leesah",
            containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    public void processNærmestelederRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        NarmesteLederHendelse hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse.class);
        digisyfoRepository.processNærmesteLederEvent(hendelse);
    }

    @KafkaListener(
            id = "min-side-arbeidsgiver-sykmelding-1",
            topics = "teamsykmelding.syfo-sendt-sykmelding",
            containerFactory = "errorLoggingKafkaListenerContainerFactory",
            batch = "true",
            properties = {
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=1000",
            }
    )
    public void processSykmeldingRecords(
            List<ConsumerRecord<String, String>> records
    ) {
        try {
            var parsedRecords = records
                    .stream()
                    .map(r ->
                            ImmutablePair.of(
                                    r.key(),
                                    getSykmeldingHendelse(r.value())
                            )
                    ).collect(Collectors.toList());
            digisyfoRepository.processSykmeldingEvent(parsedRecords);
        } catch (RuntimeException e) {
            throw new BatchListenerFailedException("exception while processing kafka event", e, records.get(0));
        }
    }

    @Nullable
    private SykmeldingHendelse getSykmeldingHendelse(String value) {
        try {
            return value == null
                    ? null
                    : objectMapper.readValue(value, SykmeldingHendelse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
