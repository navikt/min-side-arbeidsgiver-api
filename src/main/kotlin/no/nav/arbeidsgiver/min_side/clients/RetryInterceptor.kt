package no.nav.arbeidsgiver.min_side.clients

import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.retry.RetryCallback
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@SafeVarargs
fun retryInterceptor(
    maxAttempts: Int,
    backoffPeriod: Long,
    vararg retryable: Class<out Throwable>
): ClientHttpRequestInterceptor =
    ClientHttpRequestInterceptor { request, body, execution ->
        RetryTemplate().apply {
            setRetryPolicy(SimpleRetryPolicy(maxAttempts, retryable.associateWith { true }, true))
            setBackOffPolicy(FixedBackOffPolicy().apply {
                backOffPeriod = backoffPeriod
            })
        }.execute(RetryCallback {
            execution.execute(request, body)
        })
    }