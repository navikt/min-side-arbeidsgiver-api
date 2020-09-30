package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AltinnTilgangssøknad {
    String orgnr;
    String serviceCode;
    Integer serviceEdition;
    String status;
    String cratedDateTime;
    String lastChangedDateTime;
    String submitUrl;
}
