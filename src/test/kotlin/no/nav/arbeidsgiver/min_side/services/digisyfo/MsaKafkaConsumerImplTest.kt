//package no.nav.arbeidsgiver.min_side.services.digisyfo
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//import io.ktor.server.plugins.di.*
//import io.micrometer.core.instrument.MeterRegistry
//import kotlinx.coroutines.delay
//import no.nav.arbeidsgiver.min_side.FakeApplication
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.apache.kafka.clients.producer.KafkaProducer
//import org.apache.kafka.clients.producer.ProducerConfig
//import org.apache.kafka.clients.producer.ProducerRecord
//import org.apache.kafka.common.serialization.StringSerializer
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.RegisterExtension
//import org.mockito.Answers
//import org.mockito.Mockito
//import java.time.LocalDate
//import java.util.*
//TODO: kafka
//class MsaKafkaConsumerImplTest {
//    companion object {
//        @RegisterExtension
//        val app = FakeApplication(
//            addDatabase = true,
//        ) {
//            dependencies {
//                provide<DigisyfoRepository>(DigisyfoRepositoryImpl::class)
//                provide<MeterRegistry> { Mockito.mock(MeterRegistry::class.java, Answers.RETURNS_DEEP_STUBS) }
//                provide<ObjectMapper>(ObjectMapper::class)
//            }
//            startDigisyfoKafkaConsumers(this)
//        }
//    }
//
//
//    private fun createTestKafkaProducer(): KafkaProducer<String, String> {
//        val props = Properties().apply {
//            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
//            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.canonicalName)
//            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.canonicalName)
//        }
//        return KafkaProducer(props)
//    }
//
//    @Test
//    fun `objectmapper handles localdate`() = app.runTest {
//        val objectMapper = app.getDependency<ObjectMapper>()
//        objectMapper.readValue<LocalDate>("\"2020-01-01\"")
//    }
//
//
//    @Test
//    fun `sykmeldinger handles json items`() = app.runTest {
//        val kafkaProducer = createTestKafkaProducer()
//        val fnr = "0011223344556"
//        val json = """
//            {
//                "sykmelding": {
//                    "sykmeldingsperioder": [
//                        { "tom": "2022-01-01" },
//                        { "tom": "2022-03-01" }
//                    ]
//                },
//                "kafkaMetadata": {
//                    "fnr": "$fnr",
//                    "not-used": 1
//                },
//                "event": {
//                    "arbeidsgiver": {
//                        "orgnummer": "112233445"
//                    }
//                }
//            }
//        """
//        kafkaProducer.send(ProducerRecord("teamsykmelding.syfo-narmesteleder-leesah", 1, "key", json))
//        delay(2000) // wait for async to process
//        val virksomheterOgSykemeldte = app.getDependency<DigisyfoRepository>().virksomheterOgSykmeldte(fnr)
//        assertEquals(1, virksomheterOgSykemeldte.count())
//
//    }
//
//    @Test
//    fun `nærmeste leder handles json`() = app.runTest {
//        val consumerRecord = ConsumerRecord(
//            "foo", 1, 0, "someid",
//            """{
//                    "narmesteLederId": "20e8377b-9513-464a-8c09-4ebbd8c2b4e3",
//                    "fnr":"***********",
//                    "orgnummer":"974574861",
//                    "narmesteLederFnr":"***********",
//                    "narmesteLederTelefonnummer":"xxx",
//                    "narmesteLederEpost":"xxx",
//                    "aktivFom":"2020-02-24",
//                    "aktivTom":"2020-02-24",
//                    "arbeidsgiverForskutterer":true,
//                    "timestamp":"2021-05-03T07:53:33.937472Z"
//           }"""
//        )
////        kafkaConsumer.processNærmestelederRecord(consumerRecord)
//    }
//}
