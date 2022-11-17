package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public interface SykmeldingRepository {
    Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr);
    Map<String, Integer> oversiktSykmeldte(String nærmestelederFnr);

    @Data
    @AllArgsConstructor
    class Virksomhetsinfo {
        final String virksomhetsnummer;
        final Integer antallSykmeldte;
    }
    List<Virksomhetsinfo> sykmeldtePrVirksomhet(String nærmestelederFnr);

    void processEvent(List<ImmutablePair<String, SykmeldingHendelse>> records);
}
