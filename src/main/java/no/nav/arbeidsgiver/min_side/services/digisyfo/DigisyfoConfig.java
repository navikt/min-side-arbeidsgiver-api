package no.nav.arbeidsgiver.min_side.services.digisyfo;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableKafka
public class DigisyfoConfig {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, NarmesteLederHendelse> digisyfoKafkaListenerContainerFactory(
            KafkaProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, NarmesteLederHendelse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties()));
        factory.setCommonErrorHandler(new DefaultErrorHandler(new ExponentialBackOff()));
        factory.setMessageConverter(new JsonMessageConverter());
        return factory;
    }

}
