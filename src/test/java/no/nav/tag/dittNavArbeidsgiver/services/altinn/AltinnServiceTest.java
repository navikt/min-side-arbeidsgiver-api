package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import no.nav.tag.dittNavArbeidsgiver.models.Organisasjon;

public class AltinnServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    public void hentOrganisasjoner__skal_kalle_altinn_flere_ganger_ved_stor_respons() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(asList(new Organisasjon())))
            .thenReturn(ResponseEntity.ok(emptyList()));
        AltinnService altinnService = new AltinnService(new AltinnConfig(), restTemplate);
        altinnService.getFromAltinn(new ParameterizedTypeReference<List<Organisasjon>>() {},"http://blabla", 1);
        verify(restTemplate, times(1)).exchange(endsWith("&$top=1&$skip=0"), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verify(restTemplate, times(1)).exchange(endsWith("&$top=1&$skip=1"), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verifyNoMoreInteractions(restTemplate);
    }
    
}
