package no.nav.arbeidsgiver.min_side.services.ereg

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EregCacheConfig {
    @Bean
    fun eregCache() = CaffeineCache(
        EREG_CACHE,
        Caffeine.newBuilder()
            .maximumSize(600000)
            .recordStats()
            .build()
    )

    companion object {
        const val EREG_CACHE = "ereg_cache"
    }
}