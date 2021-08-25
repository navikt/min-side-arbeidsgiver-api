package no.nav.tag.dittNavArbeidsgiver.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@Configuration
public class AppConfig {

    public static final String CALL_ID = "callId";

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    /**
     * propagerer callid fra MDC til response header
     */
    @Bean
    public RestTemplateCustomizer callIdRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().addIfAbsent(CALL_ID, MDC.get(CALL_ID));
            return execution.execute(request, body);
        });
    }

    /**
     * propagerer callId, inkl varianter, fra request header til MDC, setter ny uuid hvis mangler.
     */
    @Bean
    public OncePerRequestFilter callIdTilMdcFilter() {
        List<String> kjenteHeaderNavn = List.of(
                "X-Request-ID",
                "X-Correlation-ID",
                CALL_ID,
                "call-id",
                "call_id"
        );
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    @NotNull HttpServletRequest request,
                    @NotNull HttpServletResponse response,
                    @NotNull FilterChain chain) throws ServletException, IOException
            {
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
}
