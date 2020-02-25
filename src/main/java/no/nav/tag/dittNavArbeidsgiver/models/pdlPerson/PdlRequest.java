package no.nav.tag.dittNavArbeidsgiver.models.pdlPerson;

import lombok.Value;

@Value
public class PdlRequest {
    private final String query;
    private final Variables variables;
}
