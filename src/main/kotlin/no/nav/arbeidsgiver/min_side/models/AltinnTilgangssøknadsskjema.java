package no.nav.arbeidsgiver.min_side.models;

import java.util.Objects;

public class AltinnTilgangssøknadsskjema {
    public String orgnr;
    public String redirectUrl;
    public String serviceCode;
    public Integer serviceEdition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AltinnTilgangssøknadsskjema that = (AltinnTilgangssøknadsskjema) o;
        return Objects.equals(orgnr, that.orgnr) && Objects.equals(redirectUrl, that.redirectUrl) && Objects.equals(serviceCode, that.serviceCode) && Objects.equals(serviceEdition, that.serviceEdition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgnr, redirectUrl, serviceCode, serviceEdition);
    }
}
