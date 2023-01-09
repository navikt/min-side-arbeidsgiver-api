package no.nav.arbeidsgiver.min_side.services.pushboks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksRepository.Pushboks
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PushboksService(
    val pushboksProducer: PushboksProducer,
    val repository: PushboksRepository,
    val objectMapper: ObjectMapper,
) {
    fun hent(virksomhetsnummer: String): List<Pushboks> =
        repository.hent(virksomhetsnummer)

    fun upsert(tjeneste: String, virksomhetsnummer: String, innhold: String?) {
        innhold?.let { objectMapper.readValue<JsonNode?>(it) } // ensures is proper json
        pushboksProducer.send(tjeneste, virksomhetsnummer, innhold)
    }
}


interface PushboksProducer {
    fun send(tjeneste: String, virksomhetsnummer: String, innhold: String?)
}

const val PUSHBOKS_TOPIC = "fager.msa-pushboks"

@Component
@Profile(
    "dev-gcp",
    "prod-gcp",
    "local-kafka",
)
class PushboksProducerImpl(
    val objectMapper: ObjectMapper,
    val kafkaTemplate: KafkaTemplate<String, String>,
) : PushboksProducer {

    override fun send(tjeneste: String, virksomhetsnummer: String, innhold: String?) {
        val key = PushboksRepository.PushboksKey(tjeneste = tjeneste, virksomhetsnummer = virksomhetsnummer)

        kafkaTemplate.send(PUSHBOKS_TOPIC, objectMapper.writeValueAsString(key), innhold).get(5, TimeUnit.SECONDS)
    }
}

@ConditionalOnMissingBean(PushboksProducer::class)
@Component
@Profile(
    "local",
    "labs",
)
class PushboksProducerStub(
    val pushboksRepository: PushboksRepository,
) : PushboksProducer {

    override fun send(tjeneste: String, virksomhetsnummer: String, innhold: String?) {
        val key = PushboksRepository.PushboksKey(tjeneste = tjeneste, virksomhetsnummer = virksomhetsnummer)

        pushboksRepository.processEvent(key, innhold)
    }
}

@Component
@Profile(
    "dev-gcp",
    "prod-gcp",
    "local-kafka",
)
class PushboksConsumer(
    val objectMapper: ObjectMapper,
    val repository: PushboksRepository,
) {
    @KafkaListener(
        id = "min-side-arbeidsgiver-pushboks-model-builder-1",
        topics = [PUSHBOKS_TOPIC],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processHendelse(record: ConsumerRecord<String, String?>) {
        val key: PushboksRepository.PushboksKey = objectMapper.readValue(record.key())

        repository.processEvent(key, record.value())
    }

}