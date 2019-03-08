package no.nav.tag.dittNavArbeidsgiver.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization {
    private String name;
    private String type;
    private String organizationNumber;
    private String organizationForm;
    private String status;

    public Organization() {
    }

    public String getName() {
        return name;
    }

    public void setName(String Name) {
        name = Name;
    }

    public String getType() {
        return type;
    }

    public void setType(String Type) {
        type = Type;
    }

    public String getOrganizationNumber() {
        return organizationNumber;
    }

    public void setOrganizationNumber(String OrganizationNumber) {
        organizationNumber = OrganizationNumber;
    }

    public String getOrganizationForm() {
        return organizationForm;
    }

    public void setOrganizationForm(String OrganizationForm) {
        organizationForm = OrganizationForm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String Status) {
        status = Status;
    }

    @Override
    public String toString(){
        return "nam: "+name+", type:" +type;
    }

}
