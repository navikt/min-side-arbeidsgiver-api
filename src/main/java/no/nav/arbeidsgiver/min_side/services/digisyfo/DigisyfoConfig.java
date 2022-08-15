package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
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
                (r, ex, attempt) -> {
                    log.error("retry listener invoked due to {}", ex.getClass().getCanonicalName(), ex);
                    log.error("event fail with exception topic={} parition={} offset={} key={} exception={} attempt={}", r.topic(), r.partition(), r.offset(), r.key(), ex.getClass().getCanonicalName(), attempt, ex);
                }
        );
        factory.setCommonErrorHandler(errorHandler);
        factory.setBatchListener(true);
        return factory;
    }
}
