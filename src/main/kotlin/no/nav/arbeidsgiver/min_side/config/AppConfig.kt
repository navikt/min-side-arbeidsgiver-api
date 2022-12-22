package no.nav.arbeidsgiver.min_side.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.util.*

@Configuration
@EnableScheduling
class AppConfig {
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate = builder.build()

    /**
     * propagerer callid fra MDC til request header
     */
    @Bean
    fun callIdRestTemplateCustomizer(): RestTemplateCustomizer {
        return RestTemplateCustomizer { restTemplate: RestTemplate ->
            restTemplate.interceptors.add(
                ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
                    request.headers.addIfAbsent(CALL_ID, MDC.get(CALL_ID))
                    execution.execute(request, body!!)
                })
        }
    }

    /**
     * log basic info om request response via resttemplate
     */
    @Bean
    fun loggingInterceptorCustomizer() = RestTemplateCustomizer { restTemplate: RestTemplate ->
        restTemplate.interceptors.add(
            ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
                log.info("RestTemplate.request: {} {}{}", request.method, request.uri.host, request.uri.path)
                val response = execution.execute(request, body!!)
                log.info("RestTemplate.response: {} {}", response.statusCode, response.headers.contentLength)
                response
            })
    }

    /**
     * propagerer callId, inkl varianter, fra request header til MDC, setter ny uuid hvis mangler.
     * propagerer også callid til response header
     */
    @Bean
    fun callIdTilMdcFilter(): OncePerRequestFilter {
        val kjenteHeaderNavn = listOf(
            "X-Request-ID",
            "X-Correlation-ID",
            CALL_ID,
            "call-id",
            "call_id",
            "x_callId"
        )
        return object : OncePerRequestFilter() {
            @Throws(ServletException::class, IOException::class)
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                chain: FilterChain
            ) {
                try {
                    val callId = kjenteHeaderNavn
                        .map { request.getHeader(it) }
                        .filter(Objects::nonNull)
                        .firstOrNull(String::isNotBlank) ?: UUID.randomUUID().toString()
                    MDC.put(CALL_ID, callId)
                    response.setHeader(CALL_ID, callId)
                    chain.doFilter(request, response)
                } finally {
                    MDC.remove(CALL_ID)
                }
            }
        }
    }

    /**
     * log basic info om request response på våre endepunkter
     */
    @Bean
    fun requestResponseLoggingFilter() = object : OncePerRequestFilter() {
        @Throws(ServletException::class, IOException::class)
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            chain: FilterChain
        ) {
            try {
                log.info("servlet.request {} {}", request.method, request.requestURI)
                chain.doFilter(request, response)
            } finally {
                log.info(
                    "servlet.response {} {} => {}",
                    request.method, request.requestURI, HttpStatus.resolve(response.status)
                )
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            return request.requestURI.contains("internal/actuator")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        const val CALL_ID = "callId"
    }
}