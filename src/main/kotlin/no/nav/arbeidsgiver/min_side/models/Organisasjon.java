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
    String name;
    @JsonProperty("Type")
    String type;
    @JsonProperty("ParentOrganizationNumber")
    String parentOrganizationNumber;
    @JsonProperty("OrganizationNumber")
    public String organizationNumber;
    @JsonProperty("OrganizationForm")
    String organizationForm;
    @JsonProperty("Status")
    String status;
}
