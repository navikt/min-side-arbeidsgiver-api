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
    private String varslingskodeForklaring;
    @JsonProperty("varslingskode")
    @Setter(AccessLevel.NONE)
    private String varslingskode;
    @JsonProperty("yrke")
    private String yrke;
    @JsonProperty("yrkesbeskrivelse")
    private String yrkesbeskrivelse;
    private void setVarslingskode(String varslingskode){
        this.varslingskode=varslingskode;
        if(varslingskode!=null) {
            this.varslingskodeForklaring = varselKodeOppslag.get(varslingskode);
        }
    }
@JsonIgnore
    private Map<String,String> varselKodeOppslag = Map.of(
            "ERKONK","Maskinell sluttdato: Konkurs",
            "EROPPH","Maskinell sluttdato: Opphørt i Enhetsregisteret",
            "ERVIRK","Maskinell sluttdato: Virksomhetoverdragelse",
            "IBARBG","Maskinell sluttdato: Ikke bekreftet",
            "IBKAOR","Maskinell sluttdato: Ikke bekreftet i a-ordningen");
    /*private void setVarslingskodeForklaring(){
        this.varslingskodeForklaring = "forklaring";
    };
    private String getVarslingskodeForklaring(){
            return "forklaring";
    };*/
}