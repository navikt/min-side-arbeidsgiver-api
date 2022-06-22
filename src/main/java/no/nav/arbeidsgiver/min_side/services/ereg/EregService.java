package no.nav.arbeidsgiver.min_side.services.ereg;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

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
                .build();
    }

    @Cacheable(EREG_CACHE)
    public Organisasjon hentUnderenhet(String virksomhetsnummer) {
        try {
            JsonNode json = restTemplate
                    .getForEntity("/v1/organisasjon/{virksomhetsnummer}?inkluderHierarki=true", JsonNode.class, Map.of("virksomhetsnummer", virksomhetsnummer))
                    .getBody();
            return underenhet(json);
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    @Cacheable(EREG_CACHE)
    public Organisasjon hentOverenhet(String orgnummer) {
        try {
            JsonNode json = restTemplate
                    .getForEntity("/v1/organisasjon/{orgnummer}", JsonNode.class, Map.of("orgnummer", orgnummer))
                    .getBody();
            return overenhet(json);
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == 404) {
                return null;
            }
            throw e;
        }

    }

    Organisasjon underenhet(JsonNode json) {
        if (json == null) {
            return null;
        }

        String juridiskOrgnummer = json.at("/inngaarIJuridiskEnheter/0/organisasjonsnummer").asText();
        String orgleddOrgnummer = json.at("/bestaarAvOrganisasjonsledd/0/organisasjonsledd/organisasjonsnummer").asText();
        String orgnummerTilOverenhet = orgleddOrgnummer.isBlank() ? juridiskOrgnummer : orgleddOrgnummer;
        return new Organisasjon(
                json.at("/navn/redigertnavn").asText(),
                "Business",
                orgnummerTilOverenhet,
                json.at("/organisasjonsnummer").asText(),
                json.at("/organisasjonDetaljer/enhetstyper/0/enhetstype").asText(),
                "Active"
        );
    }

    Organisasjon overenhet(JsonNode json) {
        if (json == null) {
            return null;
        }

        return new Organisasjon(
                json.at("/navn/redigertnavn").asText(),
                "Enterprise",
                null,
                json.at("/organisasjonsnummer").asText(),
                json.at("/organisasjonDetaljer/enhetstyper/0/enhetstype").asText(),
                "Active"
        );
    }

}
