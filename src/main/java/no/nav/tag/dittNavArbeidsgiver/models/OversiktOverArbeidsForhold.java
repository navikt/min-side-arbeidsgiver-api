package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OversiktOverArbeidsForhold {
    @JsonProperty("antall")
    private int antall;

    @JsonProperty("arbeidsforholdoversikter")
    private ArbeidsForhold[] arbeidsforholdoversikter;
    @JsonProperty("startrad")
    private String startrad;
    @JsonProperty("totalAntall")
    private int totalAntall;

    @JsonIgnore
    public String getAktorIDtilArbeidstaker(){
        return this.arbeidsforholdoversikter[0].getArbeidstaker().getAktoerId();
    }
}

@Data
class Arbeidsgiver {
    @JsonProperty("type")
    private String type;
}

@Data
class Opplysningspliktig {
    @JsonProperty("type")
    private String type;
}
