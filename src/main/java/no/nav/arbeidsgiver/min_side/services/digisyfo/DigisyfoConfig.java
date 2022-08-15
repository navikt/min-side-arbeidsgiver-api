package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.ExponentialBackOff;

@Profile({"dev-gcp", "prod-gcp"})
@Configuration
@EnableKafka
@Slf4j
public class DigisyfoConfig {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> digisyfoNaermesteLederKafkaListenerContainerFactory(
            KafkaProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties()));
        factory.setCommonErrorHandler(new DefaultErrorHandler(new ExponentialBackOff()));
        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> digisyfoSykmeldingKafkaListenerContainerFactory(
            KafkaProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new ExponentialBackOff());
        errorHandler.setRetryListeners(
                new RetryListener() {
                    @Override
                    public void failedDelivery(ConsumerRecord<?, ?> r, Exception ex, int attempt) {
                        log.error("retry listener invoked due to {}", ex.getClass().getCanonicalName(), ex);
                        log.error("event fail with exception topic={} parition={} offset={} key={} exception={} attempt={}", r.topic(), r.partition(), r.offset(), r.key(), ex.getClass().getCanonicalName(), attempt, ex);
                    }

                    @Override
                    public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                        log.error("retry listener recovered {}", ex.getClass().getCanonicalName(), ex);
                        RetryListener.super.recovered(record, ex);
                    }

                    @Override
                    public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
                        log.error("retry listener recoveryFailed {} {}", original.getClass().getCanonicalName(), failure.getClass().getCanonicalName(), failure);
                        RetryListener.super.recoveryFailed(record, original, failure);
                    }
                }
        );
        factory.setCommonErrorHandler(errorHandler);
        factory.setRecordInterceptor(new RecordInterceptor<String, String>() {
            @Override
            public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record) {
                return record;
            }

            @Override
            public void success(ConsumerRecord<String, String> record, Consumer<String, String> consumer) {
                log.info("record interceptor: success");
            }

            @Override
            public void failure(ConsumerRecord<String, String> record, Exception exception, Consumer<String, String> consumer) {
                log.error("record interceptor: failure {}", exception.getClass().getCanonicalName(), exception);
            }
        });
        factory.setBatchInterceptor(new BatchInterceptor<>() {
            @Override
            public ConsumerRecords<String, String> intercept(ConsumerRecords<String, String> records, Consumer<String, String> consumer) {
                return records;
            }

            @Override
            public void success(ConsumerRecords<String, String> records, Consumer<String, String> consumer) {
                log.info("batch interceptor: success");
            }

            @Override
            public void failure(ConsumerRecords<String, String> records, Exception exception, Consumer<String, String> consumer) {
                log.error("batch interceptor: failure {}", exception.getClass().getCanonicalName(), exception);
            }
        });
        factory.setBatchListener(true);
        return factory;
    }
}
