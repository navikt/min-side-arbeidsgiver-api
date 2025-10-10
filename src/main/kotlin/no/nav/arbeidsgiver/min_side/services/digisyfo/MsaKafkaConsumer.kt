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
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.lang.System.getenv
import java.time.LocalDateTime
import java.util.*

data class KafkaConsumerConfig(
    val topics: Set<String>,
    val groupId: String
)

class MsaKafkaConsumer(
    private val config: KafkaConsumerConfig,
) {
    private val properties = Properties().apply {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, (getenv("KAFKA_BROKERS") ?: "localhost:9092"))
        put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "6000")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer::class.java.canonicalName)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringSerializer::class.java.canonicalName)
        if (!getenv("KAFKA_KEYSTORE_PATH").isNullOrBlank()) {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, getenv("KAFKA_KEYSTORE_PATH"))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, getenv("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getenv("KAFKA_TRUSTSTORE_PATH"))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getenv("KAFKA_CREDSTORE_PASSWORD"))
        }
    }

    suspend fun consume(processor: ConsumerRecordProcessor) {
        val consumer = KafkaConsumer<String?, String?>(properties)
        consumer.subscribe(config.topics)
        while (true) {
            val records = consumer.poll(java.time.Duration.ofMillis(1000))
            for (record in records) {
                processor.processRecord(record)
            }
        }
    }

    suspend fun batchConsume(processor: ConsumerRecordProcessor) {
        val consumer = KafkaConsumer<String?, String?>(properties)
        consumer.subscribe(config.topics)
        while (true) {
            val records = consumer.poll(java.time.Duration.ofMillis(1000))
            if (records.any()) {
                processor.processRecords(records)
            }
        }
    }
}


interface ConsumerRecordProcessor {
    suspend fun processRecord(record: ConsumerRecord<String?, String?>)
    suspend fun processRecords(records: ConsumerRecords<String?, String?>) {
        for (record in records) {
            processRecord(record)
        }
    }
}


class NaermesteLederRecordProcessor(
    private val objectMapper: ObjectMapper,
    private val digisyfoRepository: DigisyfoRepository,
) : ConsumerRecordProcessor {
    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        val hendelse = objectMapper.readValue(record.value(), NarmesteLederHendelse::class.java)
        digisyfoRepository.processNærmesteLederEvent(hendelse)
    }
}


class SykmeldingRecordProcessor(
    private val objectMapper: ObjectMapper,
    private val digisyfoRepository: DigisyfoRepository,
) : ConsumerRecordProcessor {
    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        throw NotImplementedError("NotImplemented")
    }

    override suspend fun processRecords(records: ConsumerRecords<String?, String?>) {
        val parsedRecords = records
            .map {
                it.key() to getSykmeldingHendelse(it.value())
            }
        digisyfoRepository.processSykmeldingEvent(parsedRecords)
    }

    private fun getSykmeldingHendelse(value: String?): SykmeldingHendelse? {
        return try {
            if (value == null) null else objectMapper.readValue(value, SykmeldingHendelse::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}


class RefusjonStatusRecordProcessor(
    private val objectMapper: ObjectMapper,
    private val refusjonStatusRepository: RefusjonStatusRepository,
) : ConsumerRecordProcessor {
    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        refusjonStatusRepository.processHendelse(objectMapper.readValue(record.value()!!)) // fra spring implementasjon var dette non-nullable ConsumerRecord<String, String>
    }
}


class SykefraværStatistikkMetadataRecordProcessor(
    private val objectMapper: ObjectMapper,
    private val sykefraværstatistikkRepository: SykefraværstatistikkRepository,
) : ConsumerRecordProcessor {
    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
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

class SykefraværStatistikkRecordProcessor(
    private val objectMapper: ObjectMapper,
    private val sykefraværstatistikkRepository: SykefraværstatistikkRepository,
) : ConsumerRecordProcessor {
    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
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

class VarslingStatusRecordProcessor(
    private val objectMapper: ObjectMapper,
    private val varslingStatusRepository: VarslingStatusRepository,
) : ConsumerRecordProcessor {
    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        varslingStatusRepository.processVarslingStatus(
            objectMapper.readValue(record.value(), VarslingStatusDto::class.java)
        )
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
        MsaKafkaConsumer(config).consume(NaermesteLederRecordProcessor(objectMapper, digisyfoRepository))
    }

    // Sykemelding
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sykmelding-1",
            topics = setOf("teamsykmelding.syfo-sendt-sykmelding"),
        )
        MsaKafkaConsumer(config).batchConsume(SykmeldingRecordProcessor(objectMapper, digisyfoRepository))
    }


    // Refusjon status
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-1",
            topics = setOf("arbeidsgiver.tiltak-refusjon-endret-status"),
        )
        MsaKafkaConsumer(config).consume(RefusjonStatusRecordProcessor(objectMapper, refusjonStatusRepository))
    }

    // sykefraværstatistikk metadata
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sfmeta-3",
            topics = setOf("pia.sykefravarsstatistikk-metadata-virksomhet-v1"),
        )
        MsaKafkaConsumer(config).consume(
            SykefraværStatistikkMetadataRecordProcessor(
                objectMapper,
                sykefraværstatistikkRepository
            )
        )
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
        MsaKafkaConsumer(config).consume(
            SykefraværStatistikkRecordProcessor(
                objectMapper,
                sykefraværstatistikkRepository
            )
        )
    }

    // Varsling status
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-varsling-status-1",
            topics = setOf("fager.ekstern-varsling-status")
        )
        MsaKafkaConsumer(config).consume(VarslingStatusRecordProcessor(objectMapper, varslingStatusRepository))
    }
}