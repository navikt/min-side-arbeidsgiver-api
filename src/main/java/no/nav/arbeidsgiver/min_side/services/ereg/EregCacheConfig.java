package no.nav.arbeidsgiver.min_side.services.ereg;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class EregCacheConfig {

    final static String EREG_CACHE = "ereg_cache";

    @Bean
    public CaffeineCache eregCache(){
        return new CaffeineCache(
                EREG_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(600_000)
                        .recordStats()
                        .build()
        );
    }
}
