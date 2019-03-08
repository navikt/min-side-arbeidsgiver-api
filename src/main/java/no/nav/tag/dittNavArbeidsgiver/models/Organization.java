package no.nav.tag.dittNavArbeidsgiver.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Organization {
    private String Name;
    private String Type;
    private String OrganizationNumber;
    private String OrganizationForm;
    private String Status;
}
