package no.nav.tag.dittNavArbeidsgiver.models;

public class Role {

    private int rolleId;
    private String rolleType;
    private int rolleDefinisjonId;

    public int getRolleDefinisjonId() {
        return rolleDefinisjonId;
    }

    public void setRolleDefinisjonId(int rolleDefinisjonId) {
        this.rolleDefinisjonId = rolleDefinisjonId;
    }

    public String getRolleNavn() {
        return rolleNavn;
    }

    public void setRolleNavn(String rolleNavn) {
        this.rolleNavn = rolleNavn;
    }

    private String rolleNavn;

    public int getRolleId() {
        return rolleId;
    }

    public void setRolleId(int rolleId) {
        this.rolleId = rolleId;
    }

    public String getRolleType() {
        return rolleType;
    }

    public void setRolleType(String rolleType) {
        this.rolleType = rolleType;
    }
}
