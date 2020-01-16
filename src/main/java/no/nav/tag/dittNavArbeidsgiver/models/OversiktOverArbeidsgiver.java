package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OversiktOverArbeidsgiver {

    @JsonProperty("arbeidsgiver")
    private OversiktArbeidsgiver arbeidsgiver;

    @JsonProperty("aktiveArbeidsforhold")
    private int aktiveArbeidsforhold;
    @JsonProperty("inaktiveArbeidsforhold")
    private int inaktiveArbeidsforhold;

}

