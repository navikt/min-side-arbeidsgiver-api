package no.nav.tag.dittNavArbeidsgiver.services.yrkeskode;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class KodeverkCacheConfig {
    final static String YRKESKODE_CACHE = "yrkeskode_cache";

    @Bean
    public CaffeineCache kodeverkCache() {
        return new CaffeineCache(YRKESKODE_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(59, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
    }
}