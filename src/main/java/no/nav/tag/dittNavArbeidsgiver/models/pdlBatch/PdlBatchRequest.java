package no.nav.tag.dittNavArbeidsgiver.models.pdlBatch;

import lombok.Value;

@Value
public class PdlBatchRequest {
    private final String query;
    private final Variables variables;
}

