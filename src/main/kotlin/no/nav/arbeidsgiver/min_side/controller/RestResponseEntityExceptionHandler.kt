package no.nav.arbeidsgiver.min_side.controller

import io.ktor.client.plugins.*
import io.ktor.http.HttpStatusCode.Companion.BadGateway
import io.ktor.http.HttpStatusCode.Companion.GatewayTimeout
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.error.exceptions.AltinnrettigheterProxyKlientFallbackException
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpClientErrorException.Forbidden
import org.springframework.web.client.HttpClientErrorException.Unauthorized
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.SocketTimeoutException

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = logger()

    @ExceptionHandler(RuntimeException::class)
    @ResponseBody
    fun handleInternalError(e: RuntimeException, ignored: WebRequest?): ResponseEntity<Any> {
        log.error("Uhåndtert feil: {}", e.message, e)
        return getResponseEntity(e, "Internal error", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(AltinnrettigheterProxyKlientFallbackException::class)
    @ResponseBody
    fun handleAltinnFallbackFeil(
        e: AltinnrettigheterProxyKlientFallbackException,
        ignored: WebRequest?
    ): ResponseEntity<Any> {
        if (e.cause is SocketTimeoutException) {
            return getResponseEntity(e, "Fallback til Altinn feilet pga timeout", HttpStatus.GATEWAY_TIMEOUT)
        }
        val httpStatus = hentDriftsforstyrrelse(e)
        return if (httpStatus != null) {
            getResponseEntity(e, "Fallback til Altinn feilet pga driftsforstyrrelse", httpStatus)
        } else {
            handleInternalError(e, ignored)
        }
    }

    @ExceptionHandler(Forbidden::class)
    @ResponseBody
    fun handleForbidden(e: RuntimeException, ignored: WebRequest?) =
        getResponseEntity(e, "ingen tilgang", HttpStatus.FORBIDDEN)

    @ExceptionHandler(JwtTokenUnauthorizedException::class, Unauthorized::class)
    @ResponseBody
    fun handleUnauthorized(e: RuntimeException, ignored: WebRequest?) =
        getResponseEntity(e, "ingen tilgang", HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(OptimisticLockingFailureException::class)
    @ResponseBody
    fun handleOptimistickLockingFailure(e: RuntimeException, ignored: WebRequest?) =
        getResponseEntity(e, e.message ?: "konflikt", HttpStatus.CONFLICT)


    private fun hentDriftsforstyrrelse(e: AltinnrettigheterProxyKlientFallbackException): HttpStatus? {
        return when (e.cause) {
            is ServerResponseException -> {
                val serverResponseException = e.cause as ServerResponseException
                val status = serverResponseException.response.status
                val erDriftsforstyrrelse = listOf(
                    BadGateway,
                    ServiceUnavailable,
                    GatewayTimeout
                ).contains(status)
                if (erDriftsforstyrrelse) HttpStatus.valueOf(status.value) else null
            }

            else -> null
        }
    }

    private fun getResponseEntity(
        t: Throwable,
        melding: String,
        status: HttpStatus
    ): ResponseEntity<Any> {
        val body = FeilRespons(melding, t.message)
        log.info("Returnerer følgende HttpStatus '$status' med melding '$melding' pga exception '${t.message}'", t)
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)
    }

    data class FeilRespons(
        val message: String? = null,
        val cause: String? = null
    )

}