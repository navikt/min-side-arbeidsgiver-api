package no.nav.arbeidsgiver.min_side

import com.github.benmanes.caffeine.cache.Cache


suspend fun <K : Any, V : Any> Cache<K, Nullable<V>>.getOrComputeNullable(key: K, loader: suspend (K) -> V?): V? =
    when (val wrapper = getIfPresent(key)) {
        null -> {
            val newValue = loader(key)
            put(key, Nullable(newValue))
            newValue
        }

        else -> wrapper.value
    }

suspend fun <K : Any, V : Any> Cache<K, V>.getOrCompute(key: K, loader: suspend (K) -> V): V =
    when (val value = getIfPresent(key)) {
        null -> {
            val newValue = loader(key)
            put(key, newValue)
            newValue
        }

        else -> value
    }

// Wrapper class to allow caching null values
data class Nullable<V>(
    val value: V?
)