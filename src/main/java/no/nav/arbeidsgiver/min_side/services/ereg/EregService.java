package no.nav.arbeidsgiver.min_side.services.ereg;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
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

    private final RestTemplate restTemplate;

    public EregService(
            @Value("${ereg-services.baseUrl}") String eregBaseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        restTemplate = restTemplateBuilder
                .rootUri(eregBaseUrl)
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
        JsonNode json = restTemplate
                    .getForEntity("/v1/organisasjon/{virksomhetsnummer}?inkluderHierarki=true", JsonNode.class, Map.of("virksomhetsnummer", virksomhetsnummer))
                    .getBody();

        if (json == null) {
            return null;
        }

        return Underenhet.from(json);
    }


    @Data
    public static class Underenhet {
        final String organisasjonsnummer;
        final String navn;
        final String overordnetEnhet;
        final String slettedato;

        public static Underenhet from(JsonNode json) {
            return new Underenhet(
                    json.at("/organisasjonsnummer").asText(),
                    json.at("/navn/redigertnavn").asText(),
                    json.at("/inngaarIJuridiskEnheter/0/organisasjonsnummer").asText(),
                    null
            );
        }

        boolean erSlettet() {
            return StringUtils.isNotBlank(slettedato);
        }
    }
}
