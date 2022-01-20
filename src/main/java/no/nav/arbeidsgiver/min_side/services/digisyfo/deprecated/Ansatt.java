package no.nav.arbeidsgiver.min_side.services.digisyfo.deprecated;

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