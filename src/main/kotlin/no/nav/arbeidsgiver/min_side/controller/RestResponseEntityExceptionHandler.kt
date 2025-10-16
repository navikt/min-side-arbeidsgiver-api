package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.config.logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpClientErrorException.Forbidden
import org.springframework.web.client.HttpClientErrorException.Unauthorized
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = logger()

    @ExceptionHandler(RuntimeException::class)
    @ResponseBody
    fun handleInternalError(e: RuntimeException, ignored: WebRequest?): ResponseEntity<Any> {
        log.error("Uhåndtert feil: {}", e.message, e)
        return getResponseEntity(e, "Internal error", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(Forbidden::class)
    @ResponseBody
    fun handleForbidden(e: RuntimeException, ignored: WebRequest?) =
        getResponseEntity(e, "ingen tilgang", HttpStatus.FORBIDDEN)

    @ExceptionHandler(Unauthorized::class)
    @ResponseBody
    fun handleUnauthorized(e: RuntimeException, ignored: WebRequest?) =
        getResponseEntity(e, "ingen tilgang", HttpStatus.UNAUTHORIZED)

    /**
     * Dersom en underliggende driftsforstyrrelse bobler opp til Top Level
     * så vil denne hindre logging. Ved å propagere underliggende status så vil
     * frontend forstå at det er driftsforstyrrelse og ikke varsle det som feil.
     */
    @ExceptionHandler(
        HttpServerErrorException.ServiceUnavailable::class,
        HttpServerErrorException.BadGateway::class,
        HttpServerErrorException.GatewayTimeout::class,
    )
    fun handleDriftsforstyrrelse(ex: HttpServerErrorException) = ResponseEntity.status(ex.statusCode)

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