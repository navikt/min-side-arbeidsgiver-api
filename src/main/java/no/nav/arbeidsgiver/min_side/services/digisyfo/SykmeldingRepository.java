package no.nav.arbeidsgiver.min_side.services.digisyfo;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public interface SykmeldingRepository {
    Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr);

    void processEvent(List<ImmutablePair<String, SykmeldingHendelse>> records);
}
