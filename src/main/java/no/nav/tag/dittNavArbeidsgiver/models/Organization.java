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

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getType() {
        return Type;
    }

    public void setType(String Type) {
        this.Type = Type;
    }

    public String getOrganizationNumber() {
        return OrganizationNumber;
    }

    public void setOrganizationNumber(String OrganizationNumber) {
        this.OrganizationNumber = OrganizationNumber;
    }

    public String getOrganizationForm() {
        return OrganizationForm;
    }

    public void setOrganizationForm(String OrganizationForm) {
        this.OrganizationForm = OrganizationForm;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String Status) {
        this.Status = Status;
    }

}
