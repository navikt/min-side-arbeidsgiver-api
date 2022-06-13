package no.nav.arbeidsgiver.min_side.services.ereg;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
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
import java.util.stream.Stream;

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
    public Organisasjon hentUnderenhet(String virksomhetsnummer) {
        JsonNode json = restTemplate
                    .getForEntity("/v1/organisasjon/{virksomhetsnummer}?inkluderHierarki=true", JsonNode.class, Map.of("virksomhetsnummer", virksomhetsnummer))
                    .getBody();

        if (json == null) {
            return null;
        }

        return underenhet(json);
    }

    @Cacheable(EREG_CACHE)
    public Organisasjon hentOverenhet(String orgnummer) {
        JsonNode json = restTemplate
                    .getForEntity("/v1/organisasjon/{orgnummer}", JsonNode.class, Map.of("orgnummer", orgnummer))
                    .getBody();

        if (json == null) {
            return null;
        }

        return overenhet(json);
    }

    Organisasjon underenhet(JsonNode json) {
        return new Organisasjon(
                json.at("/navn/redigertnavn").asText(),
                "Business",
                json.at("/inngaarIJuridiskEnheter/0/organisasjonsnummer").asText(),
                json.at("/organisasjonsnummer").asText(),
                "BEDR",
                "Active"
        );
    }

    Organisasjon overenhet(JsonNode json) {
        return new Organisasjon(
                json.at("/navn/redigertnavn").asText(),
                "Enterprise",
                null,
                json.at("/organisasjonsnummer").asText(),
                json.at("/juridiskEnhetDetaljer/enhetstype").asText(),
                "Active"
        );
    }

}
