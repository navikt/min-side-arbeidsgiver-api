package no.nav.arbeidsgiver.min_side.services.digisyfo;

import java.util.Map;

public interface SykmeldingRepository {
    Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr);
}
