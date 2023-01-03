package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.stream.Collectors

/**
 * Se [...](https://github.com/navikt/sykmeldinger-arbeidsgiver/blob/main/src/main/kotlin/no/nav/syfo/sykmelding/kafka/model/SykmeldingArbeidsgiverKafkaMessage.kt)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SykmeldingHendelse(
    var sykmelding: ArbeidsgiverSykmelding? = null,
    var kafkaMetadata: KafkaMetadataDTO? = null,
    var event: SykmeldingStatusKafkaEventDTO? = null
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ArbeidsgiverSykmelding(var sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SykmeldingsperiodeAGDTO(var tom: LocalDate? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class KafkaMetadataDTO(@field:JsonProperty("fnr") var fnrAnsatt: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SykmeldingStatusKafkaEventDTO(var arbeidsgiver: ArbeidsgiverStatusDTO? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ArbeidsgiverStatusDTO(@field:JsonProperty("orgnummer") var virksomhetsnummer: String? = null)

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