package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import no.finn.unleash.Unleash;
import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;

public class AltinnServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    public void hentOrganisasjoner__skal_kalle_altinn_flere_ganger_ved_stor_respons() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TokenUtils tokenUtils = mock(TokenUtils.class);
        Unleash unleash = mock(Unleash.class);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(asList(new Organisasjon())))
            .thenReturn(ResponseEntity.ok(emptyList()));
        AltinnService altinnService = new AltinnService(getAltinnConfigForTest(), restTemplate, tokenUtils, unleash);
        altinnService.getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},"http://blabla", 1, new HttpEntity<>(null));
        verify(restTemplate, times(1)).exchange(endsWith("&$top=1&$skip=0"), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verify(restTemplate, times(1)).exchange(endsWith("&$top=1&$skip=1"), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verifyNoMoreInteractions(restTemplate);
    }

    @NotNull
    private AltinnConfig getAltinnConfigForTest() {
        AltinnConfig altinnConfig = new AltinnConfig();
        altinnConfig.setAltinnHeader("TEST");
        altinnConfig.setAltinnurl("localhost/altinn");
        altinnConfig.setAPIGwHeader("API_GW_Test");
        altinnConfig.setProxyUrl("localhost/altinn-rettigheter-proxy");
        altinnConfig.setProxyFallbackUrl("localhost/altinn");

        return altinnConfig;
    }
}
