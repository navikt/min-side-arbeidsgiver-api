package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ArbeidsForhold {
    @JsonProperty("ansattFom")
    private String ansattFom;
    @JsonProperty("ansattTom")
    private String ansattTom;
    @JsonProperty("arbeidsgiver")
    private Arbeidsgiver arbeidsgiver;
    @JsonProperty("arbeidstaker")
    private Arbeidstaker arbeidstaker;
    @JsonProperty("innrapportertEtterAOrdningen")
    private String innrapportertEtterAOrdningen;
    @JsonProperty("navArbeidsforholdId")
    private String navArbeidsforholdId;
    @JsonProperty("opplysningspliktig")
    private Opplysningspliktig opplysningspliktig;
    @JsonProperty("permisjonPermitteringsprosent")
    private String permisjonPermitteringsprosent;
    @JsonProperty("sistBekreftet")
    private String sistBekreftet;
    @JsonProperty("stillingsprosent")
    private String stillingsprosent;
    @JsonProperty("type")
    private String type;
    @JsonProperty("varsler")
    private ArbeidsForholdVarsel[] varsler;
    @JsonProperty("yrke")
    private String yrke;
    @JsonProperty("yrkesbeskrivelse")
    private String yrkesbeskrivelse;

}