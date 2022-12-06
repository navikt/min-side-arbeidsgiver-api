package no.nav.arbeidsgiver.min_side.services.digisyfo

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import spock.lang.Specification

import java.time.LocalDate

class DigisyfoKafkaConsumerImplTest extends Specification {
    def repository = new DigisyfoRepositoryStub()
    def objectMapper = new Jackson2ObjectMapperBuilder().build()
    def kafkaConsumer = new DigisyfoKafkaConsumerImpl(objectMapper, repository)

    def "objectmapper handles localdate"() {
        given:
        def date = "\"2020-01-01\""
        expect:
        objectMapper.readValue(date, LocalDate.class)
    }

    def "sykmeldinger handles empty batch"() {
        given:
        expect:
        kafkaConsumer.processSykmeldingRecords([])
    }

    def "sykmeldinger handles json items"() {
        given:
        def json = """
            {
                "sykmelding": {
                    "sykmeldingsperioder": [
                        { "tom": "2022-01-01" },
                        { "tom": "2022-03-01" }
                    ]
                },
                "kafkaMetadata": {
                    "fnr": "0011223344556",
                    "not-used": 1
                },
                "event": {
                    "arbeidsgiver": {
                        "orgnummer": "112233445"
                    }
                }
            }
        """

        expect:
        kafkaConsumer.processSykmeldingRecords([new ConsumerRecord<String, String> ("topic", 1, 1, "key", json)])
    }

    def "nærmeste leder handles json"() {
        given:
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(
                "foo", 1, 0, "someid",
                """{
                            "narmesteLederId": "20e8377b-9513-464a-8c09-4ebbd8c2b4e3",
                            "fnr":"***********",
                            "orgnummer":"974574861",
                            "narmesteLederFnr":"***********",
                            "narmesteLederTelefonnummer":"xxx",
                            "narmesteLederEpost":"xxx",
                            "aktivFom":"2020-02-24",
                            "aktivTom":"2020-02-24",
                            "arbeidsgiverForskutterer":true,
                            "timestamp":"2021-05-03T07:53:33.937472Z"
                       }"""
        );
        expect:
        kafkaConsumer.processNærmestelederRecord(consumerRecord);
    }
}
