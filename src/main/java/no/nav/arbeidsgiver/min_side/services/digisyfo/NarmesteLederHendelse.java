package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NarmesteLederHendelse {
    public final UUID narmesteLederId;
    public final String narmesteLederFnr;
    public final String aktivTom;
    public final String virksomhetsnummer;
    public final String ansattFnr;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NarmesteLederHendelse(
            @JsonProperty("narmesteLederId") UUID narmesteLederId,
            @JsonProperty("narmesteLederFnr") String narmesteLederFnr,
            @JsonProperty("aktivTom") String aktivTom,
            @JsonProperty("orgnummer") String virksomhetsnummer,
            @JsonProperty("fnr") String ansattFnr
    ) {
        this.narmesteLederId = narmesteLederId;
        this.narmesteLederFnr = narmesteLederFnr;
        this.aktivTom = aktivTom;
        this.virksomhetsnummer = virksomhetsnummer;
        this.ansattFnr = ansattFnr;
    }
}
