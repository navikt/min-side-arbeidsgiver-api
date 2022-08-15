package no.nav.arbeidsgiver.min_side.services.digisyfo

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import spock.lang.Specification

import java.time.LocalDate

class SykmeldingKafkaConsumerImplTest extends Specification {
    def repository = new SykmeldingRepositoryStub()
    def objectMapper = new Jackson2ObjectMapperBuilder().build()
    def kafkaConsumer = new SykmeldingKafkaConsumerImpl(objectMapper, repository)

    def "handles localdate"() {
        given:
        def date = "\"2020-01-01\""
        expect:
        objectMapper.readValue(date, LocalDate.class)
    }

    def "handles empty list"() {
        given:
        expect:
        kafkaConsumer.processConsumerRecord([])
    }

    def "handles json items"() {
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
        kafkaConsumer.processConsumerRecord([new ConsumerRecord<String, String> ("topic", 1, 1, "key", json)])
    }
}
