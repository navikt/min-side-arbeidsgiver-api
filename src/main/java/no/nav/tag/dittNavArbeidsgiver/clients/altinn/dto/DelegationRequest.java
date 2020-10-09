package no.nav.tag.dittNavArbeidsgiver.clients.altinn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegationRequest {
    public String RequestStatus;
    public String OfferedBy;
    public String CoveredBy;
    public String RedirectUrl;
    public String Created;
    public String LastChanged;
    public boolean KeepSessionAlive = true;

    public List<RequestResource> RequestResources;

    @JsonProperty("_links")
    public Links links;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RequestResource {
        public String ServiceCode;
        public Integer ServiceEditionCode;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        public Link sendRequest;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        public String href;
    }
}
