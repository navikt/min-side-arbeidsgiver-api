package no.nav.arbeidsgiver.min_side.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organisasjon {
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("ParentOrganizationNumber")
    private String parentOrganizationNumber;
    @JsonProperty("OrganizationNumber")
    private String organizationNumber;
    @JsonProperty("OrganizationForm")
    private String organizationForm;
    @JsonProperty("Status")
    private String status;
}
