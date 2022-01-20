package no.nav.arbeidsgiver.min_side.services.digisyfo.deprecated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigisyfoNarmesteLederRespons {
    List<Ansatt> ansatte;
}