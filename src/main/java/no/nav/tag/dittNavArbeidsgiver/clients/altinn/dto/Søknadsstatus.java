package no.nav.tag.dittNavArbeidsgiver.clients.altinn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Søknadsstatus {
    @JsonProperty("_embedded")
    public Embedded embedded;

    public String continuationtoken;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        public List<DelegationRequest> delegationRequests;
    }
}
