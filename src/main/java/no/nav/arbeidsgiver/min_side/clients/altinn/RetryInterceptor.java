package no.nav.arbeidsgiver.min_side.clients.altinn;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.util.List;

class RetryInterceptor implements ClientHttpRequestInterceptor {
    private final RetryTemplate retryTemplate;

    @SafeVarargs
    public RetryInterceptor(int maxAttempts, long backoffPeriod, Class<? extends Throwable>... retryable) {
        retryTemplate = RetryTemplate.builder()
                .retryOn(List.of(retryable))
                .traversingCauses()
                .maxAttempts(maxAttempts)
                .fixedBackoff(backoffPeriod)
                .build();
    }

    @NotNull
    @Override
    public ClientHttpResponse intercept(
            @NotNull HttpRequest request,
            @NotNull byte[] body,
            @NotNull ClientHttpRequestExecution execution
    ) throws IOException {
        return retryTemplate.execute(_ctx -> execution.execute(request, body));
    }
}
