package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NarmesteLeder {
    String aktor;
    String orgnummer;
    String tilgangFom;
    Boolean skrivetilgang;
    String [] tilganger;
}
