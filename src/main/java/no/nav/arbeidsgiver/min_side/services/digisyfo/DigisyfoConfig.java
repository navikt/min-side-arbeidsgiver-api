package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.BackOff;
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
        DefaultErrorHandler commonErrorHandler = new DefaultErrorHandler(new ExponentialBackOff());
        commonErrorHandler.setRetryListeners(
                (consumerRecord, exception, someInteger) -> {
                    log.error("event fail with exception topic={} parition={} offset={} key={} exception={}", consumerRecord.topic(),  consumerRecord.partition(), consumerRecord.offset(), consumerRecord.key(), exception.getClass().getCanonicalName(), exception);
                }
        );
        factory.setCommonErrorHandler(commonErrorHandler);
        factory.setBatchListener(true);
        return factory;
    }
}
