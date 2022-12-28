package no.nav.arbeidsgiver.min_side.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AltinnTilgangssøknad {
    public String orgnr;
    public String serviceCode;
    public Integer serviceEdition;
    public String status;
    public String createdDateTime;
    public String lastChangedDateTime;
    public String submitUrl;
}
