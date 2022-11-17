package no.nav.arbeidsgiver.min_side.services.digisyfo;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;

public interface SykmeldingRepository {
    Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr);
    Map<String, Integer> oversiktSykmeldte(String nærmestelederFnr);

    void processEvent(List<ImmutablePair<String, SykmeldingHendelse>> records);
}
