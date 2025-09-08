package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.LocalDate

class MsaKafkaConsumerImplTest {
    val repository = DigisyfoRepositoryStub()
    val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder().build()
    val kafkaConsumer = DigisyfoKafkaConsumerImpl(objectMapper, repository)

    @Test
    fun `objectmapper handles localdate`() {
        objectMapper.readValue<LocalDate>("\"2020-01-01\"")
    }

    @Test
    fun `sykmeldinger handles empty batch`() {
        kafkaConsumer.processSykmeldingRecords(listOf())
    }

    @Test
    fun `sykmeldinger handles json items`() {
        val json = """
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
        kafkaConsumer.processSykmeldingRecords(listOf(ConsumerRecord("topic", 1, 1, "key", json)))
    }

    @Test
    fun `nærmeste leder handles json`() {
        val consumerRecord = ConsumerRecord(
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
        )
        kafkaConsumer.processNærmestelederRecord(consumerRecord)
    }
}
