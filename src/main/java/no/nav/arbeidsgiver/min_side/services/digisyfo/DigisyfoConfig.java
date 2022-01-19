package no.nav.arbeidsgiver.min_side.services.digisyfo;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.springframework.util.backoff.ExponentialBackOff.DEFAULT_INITIAL_INTERVAL;
import static org.springframework.util.backoff.ExponentialBackOff.DEFAULT_MULTIPLIER;

@Profile({"dev-gcp", "prod-gcp"})
@Configuration
@EnableKafka
public class DigisyfoConfig {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, NarmesteLederHendelse> digisyfoKafkaListenerContainerFactory(
            KafkaProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, NarmesteLederHendelse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                properties.buildConsumerProperties(),
                new StringDeserializer(),
                new JsonDeserializer<>(NarmesteLederHendelse.class, false)
        ));
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(
                        (consumerRecord, e) -> {
                            throw new RuntimeException(e);
                        },
                        new ExponentialBackOff(DEFAULT_INITIAL_INTERVAL, DEFAULT_MULTIPLIER)
                )
        );
        factory.setMessageConverter(new JsonMessageConverter());
        return factory;
    }

}
