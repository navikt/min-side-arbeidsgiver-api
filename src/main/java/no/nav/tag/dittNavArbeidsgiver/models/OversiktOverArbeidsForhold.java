package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data

public class OversiktOverArbeidsForhold {
    @JsonProperty("antall")
    private String antall;

    public class ArbeidsForhold {
        @JsonProperty("ansattFom")
        private String ansattFom;
        @JsonProperty("ansattTom")
        private String ansattTom;

        public class Arbeidsgiver {
            @JsonProperty("type")
            private String type;
        }

        public class Arbeidstaker {
            @JsonProperty("type")
            private String type;
            @JsonProperty("aktoerId")
            private String aktoerId;
            @JsonProperty("offentligIdent")
            private String offentligIdent;
        }

        @JsonProperty("arbeidsgiver")
        private Arbeidsgiver arbeidsgiver;
        @JsonProperty("arbeidstaker")
        private Arbeidstaker arbeidstaker;
        @JsonProperty("innrapportertEtterAOrdningen")
        private String innrapportertEtterAOrdningen;
        @JsonProperty("navArbeidsforholdId")
        private String navArbeidsforholdId;

        public class Opplysningspliktig {
            @JsonProperty("type")
            private String type;
        }

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
        @JsonProperty("varslingskode")
        private String varslingskode;
        @JsonProperty("yrke")
        private String yrke;
    }

    @JsonProperty("arbeidsforholdoversikter")
    private ArbeidsForhold[] arbeidsforholdoversikter;
    @JsonProperty("startrad")
    private String startrad;
    @JsonProperty("totalAntall")
    private String totalAntall;

}
