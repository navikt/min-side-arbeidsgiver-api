package no.nav.arbeidsgiver.min_side.services.altinn

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class AltinnCacheConfig {
    @Bean
    fun altinnCache(): CaffeineCache {
        return CaffeineCache(
            ALTINN_CACHE,
            Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build()
        )
    }

    @Bean
    fun altinnTjenesteCache(): CaffeineCache {
        return CaffeineCache(
            ALTINN_TJENESTE_CACHE,
            Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build()
        )
    }

    companion object {
        const val ALTINN_CACHE = "altinn_cache"
        const val ALTINN_TJENESTE_CACHE = "altinn_tjeneste_cache"
    }
}