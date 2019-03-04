package no.nav.tag.dittNavArbeidsgiver.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization {
    private String Name;
    private String Type;
    private String OrganizationNumber ;
    private String OrganizationForm;
    private String Status;

    public Organization() {
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getOrganizationNumber() {
        return OrganizationNumber;
    }

    public void setOrganizationNumber(String organizationNumber) {
        OrganizationNumber = organizationNumber;
    }

    public String getOrganizationForm() {
        return OrganizationForm;
    }

    public void setOrganizationForm(String organizationForm) {
        OrganizationForm = organizationForm;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

}
