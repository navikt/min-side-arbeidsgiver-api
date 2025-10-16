package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.plugins.di.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.provideDefaultObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Answers
import org.mockito.Mockito
import java.time.LocalDate


class MsaKafkaConsumerImplTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<DigisyfoRepository>(DigisyfoRepositoryImpl::class)
                provide<MeterRegistry> { Mockito.mock(MeterRegistry::class.java, Answers.RETURNS_DEEP_STUBS) }
                provideDefaultObjectMapper()
            }
        }
    }


    @Test
    fun `objectmapper handles localdate`() = app.runTest {
        val objectMapper = app.getDependency<ObjectMapper>()
        objectMapper.readValue<LocalDate>("\"2020-01-01\"")
    }


    @Test
    fun `sykmeldinger handles json items`() = app.runTest {
        val processor = SykmeldingRecordProcessor(
            app.getDependency<ObjectMapper>(),
            app.getDependency<DigisyfoRepository>()
        )

        val fnr = "0011223344556"
        val json = """
            {
                "sykmelding": {
                    "sykmeldingsperioder": [
                        { "tom": "2022-01-01" },
                        { "tom": "2022-03-01" }
                    ]
                },
                "kafkaMetadata": {
                    "fnr": "$fnr",
                    "not-used": 1
                },
                "event": {
                    "arbeidsgiver": {
                        "orgnummer": "112233445"
                    }
                }
            }
        """
        val consumerRecords = ConsumerRecords(
            mapOf(
                TopicPartition("topic", 1) to
                        listOf(
                            ConsumerRecord(
                                "topic", 1, 0, "someid",
                                json
                            )
                        )
            )
        )
        processor.processRecords(consumerRecords)
    }

    @Test
    fun `nærmeste leder handles json`() = app.runTest {
        var processor = NaermesteLederRecordProcessor(
            app.getDependency<ObjectMapper>(),
            app.getDependency<DigisyfoRepository>()
        )
        val naermesteLederFnr = "0011223344556"
        val consumerRecord = ConsumerRecord(
            "foo", 1, 0, "someid",
            """{
                    "narmesteLederId": "20e8377b-9513-464a-8c09-4ebbd8c2b4e3",
                    "fnr":"***********",
                    "orgnummer":"974574861",
                    "narmesteLederFnr":"$naermesteLederFnr",
                    "narmesteLederTelefonnummer":"xxx",
                    "narmesteLederEpost":"xxx",
                    "aktivFom":"2020-02-24",
                    "aktivTom":"2020-02-24",
                    "arbeidsgiverForskutterer":true,
                    "timestamp":"2021-05-03T07:53:33.937472Z"
           }"""
        )
        processor.processRecord(consumerRecord)
    }
}
