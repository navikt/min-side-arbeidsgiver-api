package no.nav.arbeidsgiver.min_side.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@Configuration
@EnableScheduling
public class AppConfig {

    public static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String CALL_ID = "callId";

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    /**
     * propagerer callid fra MDC til request header
     */
    @Bean
    public RestTemplateCustomizer callIdRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().addIfAbsent(CALL_ID, MDC.get(CALL_ID));
            return execution.execute(request, body);
        });
    }

    /**
     * log basic info om request response via resttemplate
     */
    @Bean
    public RestTemplateCustomizer loggingInterceptorCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add((request, body, execution) -> {
            log.info("RestTemplate.request: {} {}{}", request.getMethod(), request.getURI().getHost(), request.getURI().getPath());
            ClientHttpResponse response = execution.execute(request, body);
            log.info("RestTemplate.response: {} {}", response.getStatusCode(), response.getHeaders().getContentLength());
            return response;
        });
    }

    /**
     * propagerer callId, inkl varianter, fra request header til MDC, setter ny uuid hvis mangler.
     * propagerer også callid til response header
     */
    @Bean
    public OncePerRequestFilter callIdTilMdcFilter() {
        List<String> kjenteHeaderNavn = List.of(
                "X-Request-ID",
                "X-Correlation-ID",
                CALL_ID,
                "call-id",
                "call_id",
                "x_callId"
        );
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    @NotNull HttpServletRequest request,
                    @NotNull HttpServletResponse response,
                    @NotNull FilterChain chain) throws ServletException, IOException {
                try {
                    String callId = kjenteHeaderNavn.stream()
                            .map(request::getHeader)
                            .filter(Objects::nonNull)
                            .filter(Predicate.not(String::isBlank))
                            .findFirst()
                            .orElseGet(() -> UUID.randomUUID().toString());
                    MDC.put(CALL_ID, callId);
                    response.setHeader(CALL_ID, callId);
                    chain.doFilter(request, response);
                } finally {
                    MDC.remove(CALL_ID);
                }
            }
        };
    }

    /**
     * log basic info om request response på våre endepunkter
     */
    @Bean
    public OncePerRequestFilter requestResponseLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    @NotNull HttpServletRequest request,
                    @NotNull HttpServletResponse response,
                    @NotNull FilterChain chain) throws ServletException, IOException {
                try {
                    log.info("servlet.request {} {}", request.getMethod(), request.getRequestURI());
                    chain.doFilter(request, response);
                } finally {
                    log.info(
                            "servlet.response {} {} => {}",
                            request.getMethod(), request.getRequestURI(), HttpStatus.resolve(response.getStatus())
                    );
                }
            }

            @Override
            protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
                return request.getRequestURI().contains("internal/actuator");
            }
        };
    }
}
