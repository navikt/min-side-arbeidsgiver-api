package no.nav.tag.dittNavArbeidsgiver.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Organisasjon {
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("ParentOrganizationNumbers")
    private String parentOrganizationNumber;
    @JsonProperty("OrganizationNumber")
    private String organizationNumber;
    @JsonProperty("OrganizationForm")
    private String organizationForm;
    @JsonProperty("Status")
    private String status;
}
