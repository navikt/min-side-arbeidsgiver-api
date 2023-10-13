package no.nav.arbeidsgiver.min_side

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers

/* Based on https://github.com/mockito/mockito-kotlin */

inline fun<reified T> kotlinAny(): T = ArgumentMatchers.any<T>() ?: castNull()

fun <T >ArgumentCaptor<T>.kotlinCapture() = capture() ?: castNull()

@Suppress("UNCHECKED_CAST")
fun <T> castNull(): T = null as T
