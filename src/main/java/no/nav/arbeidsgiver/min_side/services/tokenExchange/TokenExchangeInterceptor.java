package no.nav.arbeidsgiver.min_side.services.tokenExchange;

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Profile({"local","dev-gcp","prod-gcp"})
@Component
public class TokenExchangeInterceptor implements ClientHttpRequestInterceptor {

    final TokenExchangeClient tokenExchangeClient;
    final AuthenticatedUserHolder tokenUtils;

    public TokenExchangeInterceptor(
            TokenExchangeClient tokenExchangeClient,
            AuthenticatedUserHolder tokenUtils
    ) {
        this.tokenExchangeClient = tokenExchangeClient;
        this.tokenUtils = tokenUtils;
    }

    @NotNull
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            @NotNull byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        request.getHeaders().setBearerAuth(getToken().getAccess_token());
        return execution.execute(request, body);
    }

    private TokenXToken getToken() {
        return tokenExchangeClient.exchangeToken(tokenUtils.getToken());
    }
}
