package no.nav.arbeidsgiver.min_side.services.digisyfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.SerializableUUID

@Serializable
class NarmesteLederHendelse(
    @SerialName("narmesteLederId")  val narmesteLederId: SerializableUUID,
    @SerialName("narmesteLederFnr") val narmesteLederFnr: String,
    @SerialName("aktivTom") val aktivTom: String? = null,
    @SerialName("orgnummer") val virksomhetsnummer: String,
    @SerialName("fnr") val ansattFnr: String
)