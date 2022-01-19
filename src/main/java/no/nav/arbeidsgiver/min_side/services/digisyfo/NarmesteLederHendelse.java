package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NarmesteLederHendelse {
    public final String narmesteLederId;
    public final String narmesteLederFnr;
    public final String aktivTom;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NarmesteLederHendelse(
            @JsonProperty("narmesteLederId") String narmesteLederId,
            @JsonProperty("narmesteLederFnr") String narmesteLederFnr,
            @JsonProperty("aktivTom") String aktivTom
    ) {
        this.narmesteLederId = narmesteLederId;
        this.narmesteLederFnr = narmesteLederFnr;
        this.aktivTom = aktivTom;
    }
}
