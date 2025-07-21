package no.nav.arbeidsgiver.min_side.config

import no.nav.arbeidsgiver.min_side.config.RestTemplateConfig.Companion.log
import org.slf4j.Logger
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus.*
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate


/**
 * Beans av typen [RestTemplateCustomizer] blir automatisk registrert i [RestTemplateBuilder], vha [org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.restTemplateBuilderConfigurer].
 * og er dermed tilgjengelig for alle [RestTemplate] som opprettes i applikasjonen. Dette er en type automagi vi gjerne vil skrive oss ut av.
 * Som et steg på veien så er de samlet og dokumentert her.
 */
@Configuration
class RestTemplateConfig {

    /**
     * propagerer callid fra MDC til request header
     */
    @Bean
    fun callIdRestTemplateCustomizer() = RestTemplateCustomizer { restTemplate: RestTemplate ->
        restTemplate.interceptors.add(callIdInterceptor())
    }

    /**
     * log basic info om request response via resttemplate
     */
    @Bean
    fun loggingInterceptorCustomizer() = RestTemplateCustomizer { restTemplate: RestTemplate ->
        restTemplate.interceptors.add(loggingInterceptor())
    }

    companion object {
        val log: Logger = logger()
    }
}

fun loggingInterceptor(): ClientHttpRequestInterceptor =
    ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
        log.info("RestTemplate.request: {} {}{}", request.method, request.uri.host, request.uri.path)
        val response = execution.execute(request, body!!)
        log.atLevel(
            when {
                response.statusCode in setOf(
                    NOT_FOUND,
                    BAD_GATEWAY,
                    SERVICE_UNAVAILABLE,
                    GATEWAY_TIMEOUT,
                ) ->
                    Level.INFO

                response.statusCode.isError ->
                    Level.ERROR

                else ->
                    Level.INFO
            }
        ).log(
            "RestTemplate.response: {} Content-Length: {} for request {} {}{}",
            when {
                response.statusCode.isError ->
                    "${response.statusCode} ${response.statusText}"

                else ->
                    response.statusCode
            },
            response.headers.contentLength,
            request.method,
            request.uri.host,
            request.uri.path
        )
        response
    }

fun callIdInterceptor(headerName: String = CALL_ID) =
    ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
        MDC.get(CALL_ID)?.let {
            request.headers.addIfAbsent(headerName, it)
        }
        execution.execute(request, body!!)
    }

