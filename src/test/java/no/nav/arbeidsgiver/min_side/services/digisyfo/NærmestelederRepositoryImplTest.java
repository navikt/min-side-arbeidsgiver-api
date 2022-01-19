package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

class NærmestelederRepositoryImplTest {
    @Test
    void håndtererConsumerRecordUtenFeil() throws JsonProcessingException {
        NærmestelederRepositoryImpl repository = new NærmestelederRepositoryImpl(
                new ObjectMapper(),
                Mockito.mock(JdbcTemplate.class)
        );

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("foo", 1, 0, "someid", "{\"narmesteLederId\":\"20e8377b-9513-464a-8c09-4ebbd8c2b4e3\",\"fnr\":\"***********\",\"orgnummer\":\"974574861\",\"narmesteLederFnr\":\"***********\",\"narmesteLederTelefonnummer\":\"xxx\",\"narmesteLederEpost\":\"xxx\",\"aktivFom\":\"2020-02-24\",\"aktivTom\":\"2020-02-24\",\"arbeidsgiverForskutterer\":true,\"timestamp\":\"2021-05-03T07:53:33.937472Z\"}");
        repository.processConsumerRecord(consumerRecord);
    }
}