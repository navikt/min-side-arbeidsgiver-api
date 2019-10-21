package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class AltinnCacheConfig {

    final static String ALTINN_CACHE = "altinn_cache";
    final static String ALTINN_TJENESTE_CACHE = "altinn_tjeneste_cache";
    @Bean
    public CaffeineCache altinnCache(){
        return new CaffeineCache(ALTINN_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
    }
    @Bean
    public CaffeineCache altinnTjenesteCache(){
        return new CaffeineCache(ALTINN_TJENESTE_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
    }
}
