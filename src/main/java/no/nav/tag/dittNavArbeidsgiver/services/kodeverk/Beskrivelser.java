package no.nav.tag.dittNavArbeidsgiver.services.kodeverk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Beskrivelser {
    @JsonProperty("nn")
    private Sprak nn;
    @JsonProperty("nb")
    private Sprak nb;



}
