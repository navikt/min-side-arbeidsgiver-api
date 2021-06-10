package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DigisyfoNarmesteLederRespons {
    List<Ansatt> ansatte;
}
