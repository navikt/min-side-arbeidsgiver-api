package no.nav.arbeidsgiver.min_side

import com.github.benmanes.caffeine.cache.Cache
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> T.logger(): Logger = LoggerFactory.getLogger(this::class.java)


suspend fun <K: Any, V: Any>  Cache<K, V>.getOrCompute(key: K, loader: suspend (K) -> V): V =
    when (val value = getIfPresent(key)) {
        null -> {
            val newValue = loader(key)
            put(key, newValue)
            newValue
        }

        else -> value
    }