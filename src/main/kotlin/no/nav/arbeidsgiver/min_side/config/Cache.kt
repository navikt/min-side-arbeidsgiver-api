package no.nav.arbeidsgiver.min_side.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import java.util.concurrent.TimeUnit


class Cache<K : Any, V : Any>(
    expireAfterWriteSeconds: Long = 60,
    maximumSize: Long = 60000,
    configure: Caffeine<Any, Any>.() -> Caffeine<K, V> = {
        expireAfter(object : Expiry<K, V> {
            override fun expireAfterCreate(key: K, value: V, currentTime: Long): Long {
                return TimeUnit.SECONDS.toNanos(expireAfterWriteSeconds)
            }

            override fun expireAfterUpdate(key: K, value: V, currentTime: Long, currentDuration: Long): Long =
                currentDuration

            override fun expireAfterRead(key: K, value: V, currentTime: Long, currentDuration: Long): Long =
                currentDuration
        })
    }
) {
    private val cache: Cache<K, V> = Caffeine.newBuilder()
        .maximumSize(maximumSize)
        .configure()
        .build()

    suspend fun getOrCompute(key: K, loader: suspend (K) -> V): V =
        when (val value = cache.getIfPresent(key)) {
            null -> {
                val newValue = loader(key)
                cache.put(key, newValue)
                newValue
            }

            else -> value
        }
}