package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Se https://github.com/navikt/sykmeldinger-arbeidsgiver/blob/main/src/main/kotlin/no/nav/syfo/sykmelding/kafka/model/SykmeldingArbeidsgiverKafkaMessage.kt
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class SykmeldingHendelse {
    final ArbeidsgiverSykmelding sykmelding;
    final KafkaMetadataDTO kafkaMetadata;
    final SykmeldingStatusKafkaEventDTO event;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    static class ArbeidsgiverSykmelding {
        final List<SykmeldingsperiodeAGDTO> sykmeldingsperioder;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    static class SykmeldingsperiodeAGDTO {
        final LocalDate tom;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    static class KafkaMetadataDTO {
        @JsonProperty("fnr")
        final String fnrAnsatt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    static class SykmeldingStatusKafkaEventDTO {
        final ArbeidsgiverStatusDTO arbeidsgiver;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    static class ArbeidsgiverStatusDTO {
        @JsonProperty("orgnummer")
        final String virksomhetsnummer;
    }
}