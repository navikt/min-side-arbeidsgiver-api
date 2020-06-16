package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ArbeidsForholdVarsel {
    @JsonProperty("entitet")
    String entitet;
    @JsonProperty("varslingskode")
    @Setter(AccessLevel.NONE)
    private String varslingskode;

    private String varslingskodeForklaring;
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
            "IBKAOR","Maskinell sluttdato: Ikke bekreftet i a-ordningen",
            "PPIDHI", "Permisjonen/Permitteringen har id-historikk",
            "NAVEND", "NAV har opprettet eller endret arbeidsforholdet",
            "IBPPAG", "Maskinell sluttdato: Arbeidsgiver har ikke bekreftet permisjon/permitteringen.",
            "AFIDHI", "Arbeidsforholdet har id-historikk");
}
