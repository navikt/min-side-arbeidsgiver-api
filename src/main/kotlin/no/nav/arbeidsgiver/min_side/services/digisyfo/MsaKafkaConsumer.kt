package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.sykefraværstatistikk.*
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusRepository
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.LocalDateTime
import java.util.*

data class KafkaConsumerConfig(
    val topics: Set<String>,
    val groupId: String,
    val autoOffsetReset: String = "earliest",
    val maxPollRecords: Int = 500,
    val sessionTimeoutMs: Int = 30000
)

class MsaKafkaConsumer(
    private val config: KafkaConsumerConfig,
) {
    private val properties = Properties().apply {
        put(ConsumerConfig.GROUP_ID_CONFIG, config.groupId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.autoOffsetReset)
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, config.maxPollRecords)
        put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.sessionTimeoutMs)
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
    }

    fun consume(processMessage: (ConsumerRecord<String?, String?>) -> Unit) {
        val consumer = KafkaConsumer<String?, String?>(properties)
        consumer.subscribe(config.topics)
        while (true) {
            val records = consumer.poll(java.time.Duration.ofMillis(1000))
            for (record in records) {
                processMessage(record)
            }
        }
    }

    fun batchConsume(processMessages: (ConsumerRecords<String?, String?>) -> Unit) {
        val consumer = KafkaConsumer<String?, String?>(properties)
        consumer.subscribe(config.topics)
        while (true) {
            val records = consumer.poll(java.time.Duration.ofMillis(1000))
            if (records.any()) {
                processMessages(records)
            }
        }
    }
}

suspend fun Application.startKafkaConsumers(scope: CoroutineScope) {
    val objectMapper = dependencies.resolve<ObjectMapper>()
    val digisyfoRepository = dependencies.resolve<DigisyfoRepository>()
    val refusjonStatusRepository = dependencies.resolve<RefusjonStatusRepository>()
    val sykefraværstatistikkRepository = dependencies.resolve<SykefraværstatistikkRepository>()
    val varslingStatusRepository = dependencies.resolve<VarslingStatusRepository>()

    // Nærmeste leder
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            topics = setOf("teamsykmelding.syfo-narmesteleder-leesah"),
        )
        MsaKafkaConsumer(config).consume { record ->
            val hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse::class.java)
            digisyfoRepository.processNærmesteLederEvent(hendelse)
        }
    }

    // Sykmelding
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sykmelding-1",
            topics = setOf("teamsykmelding.syfo-sendt-sykmelding"),
        )
        MsaKafkaConsumer(config).batchConsume { records ->
            fun getSykmeldingHendelse(value: String?): SykmeldingHendelse? {
                return try {
                    if (value == null) null else objectMapper.readValue(value, SykmeldingHendelse::class.java)
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
            }

            val parsedRecords = records
                .map {
                    it.key() to getSykmeldingHendelse(it.value())
                }
            digisyfoRepository.processSykmeldingEvent(parsedRecords)
        }
    }

    // Refusjon status
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-1",
            topics = setOf("arbeidsgiver.tiltak-refusjon-endret-status"),
        )
        MsaKafkaConsumer(config).consume { record ->
            refusjonStatusRepository.processHendelse(objectMapper.readValue(record.value()!!)) // fra spring implementasjon var dette non-nullable ConsumerRecord<String, String>
        }
    }

    // sykefraværstatistikk metadata
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sfmeta-2",
            topics = setOf("arbeidsgiver.sykefravarsstatistikk-metadata-virksomhet-v1"),
        )
        MsaKafkaConsumer(config).consume { record ->
            val key = record.key().let {
                objectMapper.readValue(it, MetadataVirksomhetKafkaKeyDto::class.java)
            }

            if (key.arstall.toInt() >= LocalDateTime.now().year - 1) {
                sykefraværstatistikkRepository.processMetadataVirksomhet(
                    objectMapper.readValue(record.value(), MetadataVirksomhetDto::class.java)
                )
            }
        }
    }

    // sykefraværstatistikk virksomhet, næring, bransje
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sfstats-2",
            topics = setOf(
                "arbeidsgiver.sykefravarsstatistikk-virksomhet-v1",
                "arbeidsgiver.sykefravarsstatistikk-naring-v1",
                "arbeidsgiver.sykefravarsstatistikk-bransje-v1",
            )
        )
        MsaKafkaConsumer(config).consume { record ->
            val key = record.key().let {
                objectMapper.readValue(it, StatistikkategoriKafkaKeyDto::class.java)
            }

            if (key.arstall.toInt() >= LocalDateTime.now().year - 1) {
                sykefraværstatistikkRepository.processStatistikkategori(
                    objectMapper.readValue(record.value(), StatistikkategoriDto::class.java)
                )
            }
        }
    }

    // Varsling status
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-varsling-status-1",
            topics = setOf("fager.ekstern-varsling-status")
        )
        MsaKafkaConsumer(config).consume { record ->
            varslingStatusRepository.processVarslingStatus(
                objectMapper.readValue(record.value(), VarslingStatusDto::class.java)
            )
        }
    }
}