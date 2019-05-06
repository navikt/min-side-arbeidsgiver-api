package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Role {
    @JsonProperty("RoleDefinitionId")
    private int roleId;
    @JsonProperty("RoleType")
    private String roleType;
    @JsonProperty("RoleName")
    private String roleName;
    @JsonProperty("RoleDescription")
    private String roleDescription;
    @JsonProperty("RoleDefinitionCode")
    private String roleDefinitionCode;
}
