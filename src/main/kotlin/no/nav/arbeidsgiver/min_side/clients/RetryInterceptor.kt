package no.nav.arbeidsgiver.min_side.clients

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.retry.support.RetryTemplate

class RetryInterceptor(
    maxAttempts: Int,
    backoffPeriod: Long,
    vararg retryable: Class<out Throwable>
) : ClientHttpRequestInterceptor {

    private val retryTemplate = RetryTemplate.builder()
        .retryOn(listOf(*retryable))
        .traversingCauses()
        .maxAttempts(maxAttempts)
        .fixedBackoff(backoffPeriod)
        .build()

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse = retryTemplate.execute<ClientHttpResponse, Exception> { execution.execute(request, body) }
}