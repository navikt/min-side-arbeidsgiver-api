package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public
class OversiktArbeidsgiver {
    @JsonProperty("type")
    private String type;
    private String organisasjonsnummer;
}
