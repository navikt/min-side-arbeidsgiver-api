package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class NarmesteLederHendelse @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("narmesteLederId") val narmesteLederId: UUID,
    @param:JsonProperty("narmesteLederFnr") val narmesteLederFnr: String,
    @param:JsonProperty("aktivTom") val aktivTom: String?,
    @param:JsonProperty("orgnummer") val virksomhetsnummer: String,
    @param:JsonProperty("fnr") val ansattFnr: String
)