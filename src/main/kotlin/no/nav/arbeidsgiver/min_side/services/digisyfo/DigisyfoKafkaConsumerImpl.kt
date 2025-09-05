package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.BatchListenerFailedException
import org.springframework.stereotype.Component

class DigisyfoKafkaConsumerImpl(
    private val objectMapper: ObjectMapper,
    private val digisyfoRepository: DigisyfoRepository
) {

    @KafkaListener(
        id = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
        topics = ["teamsykmelding.syfo-narmesteleder-leesah"],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processNærmestelederRecord(record: ConsumerRecord<String?, String?>) {
        val hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse::class.java)
        digisyfoRepository.processNærmesteLederEvent(hendelse)
    }

    @KafkaListener(
        id = "min-side-arbeidsgiver-sykmelding-1",
        topics = ["teamsykmelding.syfo-sendt-sykmelding"],
        containerFactory = "errorLoggingKafkaListenerContainerFactory",
        batch = "true",
        properties = ["${ConsumerConfig.MAX_POLL_RECORDS_CONFIG}=1000"]
    )
    fun processSykmeldingRecords(
        records: List<ConsumerRecord<String?, String?>>
    ) {
        try {
            val parsedRecords = records
                .map {
                    it.key() to getSykmeldingHendelse(it.value())
                }
            digisyfoRepository.processSykmeldingEvent(parsedRecords)
        } catch (e: RuntimeException) {
            throw BatchListenerFailedException("exception while processing kafka event", e, records[0])
        }
    }

    private fun getSykmeldingHendelse(value: String?): SykmeldingHendelse? {
        return try {
            if (value == null) null else objectMapper.readValue(value, SykmeldingHendelse::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}