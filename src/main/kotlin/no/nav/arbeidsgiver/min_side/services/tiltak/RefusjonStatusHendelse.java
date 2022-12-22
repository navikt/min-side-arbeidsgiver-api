package no.nav.arbeidsgiver.min_side.services.tiltak;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RefusjonStatusHendelse {
    public final String refusjonId;
    public final String virksomhetsnummer;
    public final String avtaleId;
    public final String status;


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RefusjonStatusHendelse(
            @JsonProperty("refusjonId") String refusjonId,
            @JsonProperty("bedriftNr") String virksomhetsnummer,
            @JsonProperty("avtaleId") String avtaleId,
            @JsonProperty("status") String status
    ) {
        this.refusjonId = refusjonId;
        this.virksomhetsnummer = virksomhetsnummer;
        this.avtaleId = avtaleId;
        this.status = status;
    }
}
