package no.nav.tag.dittNavArbeidsgiver.services.aad;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class AadCacheConfig {

    final static String AAD_CACHE = "aad_cache";

    @Bean
    public CaffeineCache aadCache() {
        return new CaffeineCache(AAD_CACHE,
                Caffeine.newBuilder()
                    .maximumSize(1)
                    .expireAfterWrite(59, TimeUnit.MINUTES)
                    .recordStats()
                    .build());
    }
}
