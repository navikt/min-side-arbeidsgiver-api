package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.infrastruktur.logger
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.MetadataVirksomhetKafkaKeyDto
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.StatistikkategoriKafkaKeyDto
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.SykefravarstatistikkRepository
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusRepository
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
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
    private val log = logger()

    private val properties = Properties().apply {
        put(ConsumerConfig.GROUP_ID_CONFIG, config.groupId)
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, (getenv("KAFKA_BROKERS") ?: "localhost:9092"))
        put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "60000")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.canonicalName)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.canonicalName)
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

    suspend fun consume(processor: ConsumerRecordProcessor) = withContext(Dispatchers.IO) {
        KafkaConsumer<String?, String?>(properties).use { consumer ->
            consumer.subscribe(config.topics)
            log.info("Successfully subscribed to $config")

            while (isActive) {
                try {
                    val records = consumer.poll(java.time.Duration.ofMillis(1000))
                    log.info("polled {} records {}", records.count(), config)

                    if (records.any()) {
                        for (record in records) {
                            try {
                                processor.processRecord(record)
                            } catch (e: Exception) {
                                log.error("Feil ved prosessering av kafka-melding.", e)

                                // without seek next poll will advance the offset, regardless of autocommit=false
                                consumer.seek(TopicPartition(record.topic(), record.partition()), record.offset())

                                throw Exception("Feil ved prosessering av kafka-melding. partition=${record.partition()} offset=${record.offset()} $config", e)
                            }
                        }
                        log.info("committing offsets: {} {}", records.partitions().associateWith { tp -> records.records(tp).last().offset() }, config)
                        consumer.commitSync()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error("Feil ved prosessering av kafka-melding. $config", e)
                    delay(5000)
                }
            }
        }
    }

    suspend fun batchConsume(processor: ConsumerRecordProcessor) = withContext(Dispatchers.IO) {
        KafkaConsumer<String?, String?>(properties).use { consumer ->
            consumer.subscribe(config.topics)
            log.info("Successfully subscribed to $config")

            while (isActive) {
                try {
                    val records = consumer.poll(java.time.Duration.ofMillis(1000))
                    log.info("polled {} records {}", records.count(), config)

                    if (records.any()) {
                        try {
                            processor.processRecords(records)
                        } catch (e: Exception) {
                            log.error("Feil ved prosessering av kafka-melding.", e)

                            // without seek next poll will advance the offset, regardless of autocommit=false
                            for (tp in records.partitions()) {
                                consumer.seek(tp, records.records(tp).first().offset())
                            }

                            throw Exception("Feil ved prosessering av kafka-melding. $config", e)
                        }
                        log.info("committing offsets: {} {}", records.partitions().associateWith { tp -> records.records(tp).last().offset() }, config)
                        consumer.commitSync()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error("Feil ved prosessering av kafka-melding. $config", e)
                    delay(5000)
                }
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
    private val digisyfoRepository: DigisyfoRepository,
) : ConsumerRecordProcessor {

    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        val value = record.value() ?: return // tombstone, ignore

        val hendelse = defaultJson.decodeFromString<NarmesteLederHendelse>(value)
        digisyfoRepository.processNærmesteLederEvent(hendelse)
    }
}


class SykmeldingRecordProcessor(
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

    private fun getSykmeldingHendelse(value: String?): SykmeldingHendelse? =
        if (value == null) null else defaultJson.decodeFromString(value)
}


class RefusjonStatusRecordProcessor(
    private val refusjonStatusRepository: RefusjonStatusRepository,
) : ConsumerRecordProcessor {

    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        val value = record.value() ?: return // tombstone, ignore
        
        refusjonStatusRepository.processHendelse(defaultJson.decodeFromString(value)) // fra spring implementasjon var dette non-nullable ConsumerRecord<String, String>
    }
}


class SykefraværStatistikkMetadataRecordProcessor(
    private val sykefravarstatistikkRepository: SykefravarstatistikkRepository,
) : ConsumerRecordProcessor {

    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        val rKey = record.key() ?: return
        val rValue = record.value() ?: return // tombstone, ignore

        val key = defaultJson.decodeFromString<MetadataVirksomhetKafkaKeyDto>(rKey)
        if (key.arstall >= LocalDateTime.now().year - 1) {
            sykefravarstatistikkRepository.processMetadataVirksomhet(
                defaultJson.decodeFromString(rValue)
            )
        }
    }
}

class SykefraværStatistikkRecordProcessor(
    private val sykefravarstatistikkRepository: SykefravarstatistikkRepository,
) : ConsumerRecordProcessor {

    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        val rKey = record.key() ?: return
        val rValue = record.value() ?: return // tombstone, ignore
        
        val key = defaultJson.decodeFromString<StatistikkategoriKafkaKeyDto>(rKey)
        if (key.arstall >= LocalDateTime.now().year - 1) {
            sykefravarstatistikkRepository.processStatistikkategori(
                defaultJson.decodeFromString(rValue)
            )
        }
    }
}

class VarslingStatusRecordProcessor(
    private val varslingStatusRepository: VarslingStatusRepository,
) : ConsumerRecordProcessor {

    override suspend fun processRecord(record: ConsumerRecord<String?, String?>) {
        val value = record.value() ?: return // tombstone, ignore
        
        varslingStatusRepository.processVarslingStatus(
            defaultJson.decodeFromString(value)
        )
    }
}


suspend fun Application.startKafkaConsumers(scope: CoroutineScope) {
    val digisyfoRepository = dependencies.resolve<DigisyfoRepository>()
    val refusjonStatusRepository = dependencies.resolve<RefusjonStatusRepository>()
    val sykefravarstatistikkRepository = dependencies.resolve<SykefravarstatistikkRepository>()
    val varslingStatusRepository = dependencies.resolve<VarslingStatusRepository>()


    // Nærmeste leder
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-narmesteleder-model-builder-1",
            topics = setOf("teamsykmelding.syfo-narmesteleder-leesah"),
        )
        MsaKafkaConsumer(config).consume(NaermesteLederRecordProcessor(digisyfoRepository))
    }

    // Sykemelding
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sykmelding-1",
            topics = setOf("teamsykmelding.syfo-sendt-sykmelding"),
        )
        MsaKafkaConsumer(config).batchConsume(SykmeldingRecordProcessor(digisyfoRepository))
    }


    // Refusjon status
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-1",
            topics = setOf("arbeidsgiver.tiltak-refusjon-endret-status"),
        )
        MsaKafkaConsumer(config).consume(RefusjonStatusRecordProcessor(refusjonStatusRepository))
    }

    // sykefraværstatistikk metadata
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sfmeta-3",
            topics = setOf("pia.sykefravarsstatistikk-metadata-virksomhet-v1"),
        )
        MsaKafkaConsumer(config).consume(
            SykefraværStatistikkMetadataRecordProcessor(
                sykefravarstatistikkRepository
            )
        )
    }

    // sykefraværstatistikk virksomhet, næring, bransje
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-sfstats-3",
            topics = setOf(
                "pia.sykefravarsstatistikk-virksomhet-v1",
                "pia.sykefravarsstatistikk-naring-v1",
                "pia.sykefravarsstatistikk-bransje-v1",
            )
        )
        MsaKafkaConsumer(config).consume(
            SykefraværStatistikkRecordProcessor(
                sykefravarstatistikkRepository
            )
        )
    }

    // Varsling status
    scope.launch {
        val config = KafkaConsumerConfig(
            groupId = "min-side-arbeidsgiver-varsling-status-1",
            topics = setOf("fager.ekstern-varsling-status")
        )
        MsaKafkaConsumer(config).consume(VarslingStatusRecordProcessor(varslingStatusRepository))
    }
}