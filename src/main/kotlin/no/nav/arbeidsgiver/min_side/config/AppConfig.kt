package no.nav.arbeidsgiver.min_side.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus.resolve
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

const val CALL_ID = "callId"


/**
 * Beans av typen [OncePerRequestFilter] ie [jakarta.servlet.Filter] blir automatisk registrert i Spring Boot sin filter-kjede
 * og er dermed tilgjengelig for alle requests som kommer inn i applikasjonen. Dette skjer vha [org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans]
 *
 * Dette er en type automagi vi gjerne vil skrive oss ut av. Men kanskje ikke like viktig som [RestTemplateConfig] siden det kun er en servlet filter-kjede som blir opprettet per applikasjon.
 */
@Configuration
@EnableScheduling
@ConfigurationPropertiesScan("no.nav.arbeidsgiver.min_side")
class AppConfig {

    private val log = logger()

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
                    request.method, request.requestURI, resolve(response.status)
                )
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            return request.requestURI.contains("internal/actuator")
        }
    }

    @Bean
    fun characterEncodingFilter() = CharacterEncodingFilter("UTF-8", true)
}

inline fun <reified T : Any> T.logger(): Logger = LoggerFactory.getLogger(this::class.java)