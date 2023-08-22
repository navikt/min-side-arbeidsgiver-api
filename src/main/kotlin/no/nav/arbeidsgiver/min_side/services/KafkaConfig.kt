package no.nav.arbeidsgiver.min_side.services

import no.nav.arbeidsgiver.min_side.config.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.util.backoff.ExponentialBackOff

@Profile("dev-gcp", "prod-gcp")
@Configuration
@EnableKafka
class KafkaConfig {
    private val log = logger()
    @Bean
    fun errorLoggingKafkaListenerContainerFactory(
        properties: KafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        val errorHandler = DefaultErrorHandler(ExponentialBackOff())
        errorHandler.setRetryListeners(
            RetryListener { r: ConsumerRecord<*, *>, ex: Exception, attempt: Int ->
                log.error(
                    "KafkaListener failed. attempt={} topic={} parition={} offset={} key={} exception={}",
                    attempt, r.topic(), r.partition(), r.offset(), r.key(), ex.javaClass.canonicalName, ex
                )
            }
        )
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }
}