package no.nav.arbeidsgiver.min_side.services.ereg;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static no.nav.arbeidsgiver.min_side.services.ereg.EregCacheConfig.EREG_CACHE;

@Slf4j
@Component
public class EregService {
    private static final Logger logger = LoggerFactory.getLogger(lookup().lookupClass());

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
        logger.info("#hentUnderenhet({}) => {}", virksomhetsnummer, json);

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
        logger.info("#hentOverenhet({}) => {}", orgnummer, json);

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
