package no.nav.arbeidsgiver.min_side.services.digisyfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Se https://github.com/navikt/sykmeldinger-arbeidsgiver/blob/main/src/main/kotlin/no/nav/syfo/sykmelding/kafka/model/SykmeldingArbeidsgiverKafkaMessage.kt
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SykmeldingHendelse {
    public ArbeidsgiverSykmelding sykmelding;
    public KafkaMetadataDTO kafkaMetadata;
    public SykmeldingStatusKafkaEventDTO event;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ArbeidsgiverSykmelding {
        public List<SykmeldingsperiodeAGDTO> sykmeldingsperioder;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SykmeldingsperiodeAGDTO {
        public LocalDate tom;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class KafkaMetadataDTO {
        @JsonProperty("fnr")
        public String fnrAnsatt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SykmeldingStatusKafkaEventDTO {
        public ArbeidsgiverStatusDTO arbeidsgiver;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ArbeidsgiverStatusDTO {
        @JsonProperty("orgnummer")
        public String virksomhetsnummer;
    }

    static SykmeldingHendelse create(
            String fnrAnsatt,
            String virksomhetsnummer,
            List<String> toms
    ) {
        return new SykmeldingHendelse(
                new ArbeidsgiverSykmelding(
                        toms.stream()
                                .map(LocalDate::parse)
                                .map(SykmeldingsperiodeAGDTO::new)
                                .collect(Collectors.toList())
                ),
                new KafkaMetadataDTO(fnrAnsatt),
                new SykmeldingStatusKafkaEventDTO(
                        new ArbeidsgiverStatusDTO(virksomhetsnummer)
                )
        );
    }
}