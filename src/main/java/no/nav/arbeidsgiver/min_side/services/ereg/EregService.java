package no.nav.arbeidsgiver.min_side.services.ereg;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.io.IOException;
import java.util.Map;

import static no.nav.arbeidsgiver.min_side.services.ereg.EregCacheConfig.EREG_CACHE;

@Slf4j
@Component
public class EregService {

    public static final String API_URL = "https://data.brreg.no/enhetsregisteret/api/underenheter/{virksomhetsnummer}";
    private final RestTemplate restTemplate;

    public EregService(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(@NotNull ClientHttpResponse response) throws IOException {
                        return response.getStatusCode() != HttpStatus.NOT_FOUND && response.getStatusCode().isError();
                    }
                })
                .build();
    }

    @Cacheable(EREG_CACHE)
    public Underenhet hentUnderenhet(String virksomhetsnummer) {
        Underenhet underenhet = restTemplate
                    .getForEntity(API_URL, Underenhet.class, Map.of("virksomhetsnummer", virksomhetsnummer))
                    .getBody();

        if (underenhet == null || underenhet.erSlettet()) {
            return null;
        }

        return underenhet;
    }


    @Data
    public static class Underenhet {
        final String organisasjonsnummer;
        final String navn;
        final String overordnetEnhet;
        final String slettedato;

        boolean erSlettet() {
            return StringUtils.isNotBlank(slettedato);
        }
    }
}
