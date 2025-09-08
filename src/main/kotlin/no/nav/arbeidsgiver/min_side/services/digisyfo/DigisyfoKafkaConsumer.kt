package no.nav.arbeidsgiver.min_side.services.digisyfo

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*

data class KafkaConsumerConfig(
    val topics: Set<String>,
    val groupId: String,
    val autoOffsetReset: String = "earliest",
    val maxPollRecords: Int = 500,
    val sessionTimeoutMs: Int = 30000
)

class DigisyfoKafkaConsumer(
    private val config: KafkaConsumerConfig,
) {
    private val properties = Properties().apply {
        put(ConsumerConfig.GROUP_ID_CONFIG, config.groupId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.autoOffsetReset)
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, config.maxPollRecords)
        put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.sessionTimeoutMs)
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
    }

    fun consumeMessages(processMessage: (ConsumerRecord<String?, String?>) -> Unit) {
        val consumer = KafkaConsumer<String?, String?>(properties)
        consumer.subscribe(config.topics)
        while (true) {
            val records = consumer.poll(java.time.Duration.ofMillis(1000))
            for (record in records) {
                processMessage(record)
            }
        }
    }

    fun batchConsumeMessages(processMessages: (ConsumerRecords<String?, String?>) -> Unit) {
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
