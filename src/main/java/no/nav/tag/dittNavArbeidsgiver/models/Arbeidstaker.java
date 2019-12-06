package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Arbeidstaker {
    @JsonProperty("type")
    private String type;
    @JsonProperty("aktoerId")
    private String aktoerId;
    @JsonProperty("offentligIdent")
    private String offentligIdent;
    private String navn;
}
