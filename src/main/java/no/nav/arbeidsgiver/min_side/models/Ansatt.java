package no.nav.arbeidsgiver.min_side.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Ansatt {
    String fnr;
    String navn;
    String orgnummer;
    String narmestelederId;
}
