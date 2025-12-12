package no.nav.arbeidsgiver.min_side.services.digisyfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.SerializableLocalDate
import java.time.LocalDate
import java.util.stream.Collectors

/**
 * Se [...](https://github.com/navikt/sykmeldinger-arbeidsgiver/blob/main/src/main/kotlin/no/nav/syfo/sykmelding/kafka/model/SykmeldingArbeidsgiverKafkaMessage.kt)
 */

@Serializable
data class SykmeldingHendelse(
    var sykmelding: ArbeidsgiverSykmelding? = null,
    var kafkaMetadata: KafkaMetadataDTO? = null,
    var event: SykmeldingStatusKafkaEventDTO? = null
) {

    @Serializable
    data class ArbeidsgiverSykmelding(var sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>? = null)

    @Serializable
    data class SykmeldingsperiodeAGDTO(var tom: SerializableLocalDate? = null)

    @Serializable
    data class KafkaMetadataDTO(@SerialName("fnr") var fnrAnsatt: String? = null)

    @Serializable
    data class SykmeldingStatusKafkaEventDTO(var arbeidsgiver: ArbeidsgiverStatusDTO? = null)

    @Serializable
    data class ArbeidsgiverStatusDTO(@SerialName("orgnummer") var virksomhetsnummer: String? = null)

    companion object {
        fun create(
            fnrAnsatt: String?,
            virksomhetsnummer: String?,
            toms: List<String?>
        ): SykmeldingHendelse {
            return SykmeldingHendelse(
                ArbeidsgiverSykmelding(
                    toms.stream()
                        .map { text: String? -> LocalDate.parse(text) }
                        .map { tom: LocalDate? -> SykmeldingsperiodeAGDTO(tom) }
                        .collect(Collectors.toList())
                ),
                KafkaMetadataDTO(fnrAnsatt),
                SykmeldingStatusKafkaEventDTO(
                    ArbeidsgiverStatusDTO(virksomhetsnummer)
                )
            )
        }
    }
}