package no.nav.arbeidsgiver.min_side.services.ereg;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
        if (json == null || json.isEmpty()) {
            return null;
        }

        String juridiskOrgnummer = json.at("/inngaarIJuridiskEnheter/0/organisasjonsnummer").asText();
        String orgleddOrgnummer = json.at("/bestaarAvOrganisasjonsledd/0/organisasjonsledd/organisasjonsnummer").asText();
        String orgnummerTilOverenhet = orgleddOrgnummer.isBlank() ? juridiskOrgnummer : orgleddOrgnummer;
        return new Organisasjon(
                samletNavn(json),
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
                samletNavn(json),
                "Enterprise",
                null,
                json.at("/organisasjonsnummer").asText(),
                json.at("/organisasjonDetaljer/enhetstyper/0/enhetstype").asText(),
                "Active"
        );
    }

    @NotNull
    private static String samletNavn(JsonNode json) {
        return Stream.of(
                json.at("/navn/navnelinje1").asText(null),
                json.at("/navn/navnelinje2").asText(null),
                json.at("/navn/navnelinje3").asText(null),
                json.at("/navn/navnelinje4").asText(null),
                json.at("/navn/navnelinje5").asText(null)
        )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

}
