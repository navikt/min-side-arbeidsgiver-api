package no.nav.arbeidsgiver.min_side.controller;

import io.ktor.client.features.ServerResponseException;
import io.ktor.http.HttpStatusCode;
import lombok.Data;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.error.exceptions.AltinnrettigheterProxyKlientFallbackException;
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
import no.nav.arbeidsgiver.min_side.exceptions.TilgangskontrollException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.SocketTimeoutException;
import java.util.List;

import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(lookup().lookupClass());

    @ExceptionHandler({RuntimeException.class})
    @ResponseBody
    protected ResponseEntity<Object> handleInternalError(RuntimeException e, WebRequest ignored) {
        logger.error("Uhåndtert feil: {}", e.getMessage(), e);
        return getResponseEntity(e, "Internal error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AltinnrettigheterProxyKlientFallbackException.class})
    @ResponseBody
    protected ResponseEntity<Object> handleAltinnFallbackFeil(AltinnrettigheterProxyKlientFallbackException e, WebRequest ignored) {
        if (e.getCause() instanceof SocketTimeoutException) {
            return getResponseEntity(e, "Fallback til Altinn feilet pga timeout", HttpStatus.GATEWAY_TIMEOUT);
        }

        HttpStatus httpStatus = hentDriftsforstyrrelse(e);
        if (httpStatus != null) {
            return getResponseEntity(e, "Fallback til Altinn feilet pga driftsforstyrrelse", httpStatus);
        } else {
            return handleInternalError(e, ignored);
        }
    }

    private HttpStatus hentDriftsforstyrrelse(AltinnrettigheterProxyKlientFallbackException e) {
        if (e.getCause() instanceof ServerResponseException) {
            ServerResponseException serverResponseException = (ServerResponseException) e.getCause();
            HttpStatusCode status = serverResponseException.getResponse().getStatus();
            boolean erDriftsforstyrrelse = List.of(
                    HttpStatusCode.Companion.getBadGateway(),
                    HttpStatusCode.Companion.getServiceUnavailable(),
                    HttpStatusCode.Companion.getGatewayTimeout()
            ).contains(status);
            return erDriftsforstyrrelse ? HttpStatus.valueOf(status.getValue()) : null;
        } else {
            return null;
        }
    }

    @ExceptionHandler({
            TilgangskontrollException.class,
            HttpClientErrorException.Forbidden.class,
    })
    @ResponseBody
    protected ResponseEntity<Object> handleForbidden(RuntimeException e, WebRequest ignored) {
        return getResponseEntity(e, "ingen tilgang", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            JwtTokenUnauthorizedException.class,
            HttpClientErrorException.Unauthorized.class,
    })
    @ResponseBody
    protected ResponseEntity<Object> handleUnauthorized(RuntimeException e, WebRequest ignored) {
        return getResponseEntity(e, "ingen tilgang", HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Object> getResponseEntity(
            Throwable t,
            String melding,
            HttpStatus status
    ) {
        FeilRespons body = new FeilRespons(melding, t.getMessage());
        logger.info(
                format(
                        "Returnerer følgende HttpStatus '%s' med melding '%s' pga exception '%s'",
                        status.toString(),
                        melding,
                        t.getMessage()
                ), t
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @Data
    public static class FeilRespons {
        final String message;
        final String cause;
    }

}