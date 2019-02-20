package no.nav.tag.dittNavArbeidsgiver.controller;


import java.io.Serializable;

public class Organization {
    private String navn;
    private String type;
    private String orgNo;
    private String overordnetOrgNo;
    private String status;

    public Organization() {
    }
    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrgNo() {
        return orgNo;
    }

    public void setOrgNo(String orgNo) {
        this.orgNo = orgNo;
    }

    public String getOverordnetOrgNo() {
        return overordnetOrgNo;
    }

    public void setOverordnetOrgNo(String overordnetOrgNo) {
        this.overordnetOrgNo = overordnetOrgNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
