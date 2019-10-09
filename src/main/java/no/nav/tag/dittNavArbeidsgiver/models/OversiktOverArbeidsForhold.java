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











        @JsonProperty("ParentOrganizationNumber")
        private String parentOrganizationNumber;
        @JsonProperty("OrganizationNumber")
        private String organizationNumber;
        @JsonProperty("OrganizationForm")
        private String organizationForm;
        @JsonProperty("Status")
        private String status;
    }

    @JsonProperty("Type")
    private String type;
    @JsonProperty("ParentOrganizationNumber")
    private String parentOrganizationNumber;
    @JsonProperty("OrganizationNumber")
    private String organizationNumber;
    @JsonProperty("OrganizationForm")
    private String organizationForm;
    @JsonProperty("Status")
    private String status;
}
