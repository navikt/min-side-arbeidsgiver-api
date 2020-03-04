package no.nav.tag.dittNavArbeidsgiver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class ConcurrencyConfig {

    @Bean("someExecutor")
    public Executor hentNavnExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(7);
        executor.setQueueCapacity(100);
        executor.initialize();
        executor.setThreadNamePrefix("MSA-AAREG-Thread-");
        return executor;
    }
}
