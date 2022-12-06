package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Profile({"local", "labs"})
@Slf4j
@Repository
public class SykmeldingRepositoryStub implements SykmeldingRepository {
    @Override
    public Map<String, Integer> oversiktSykmeldinger(String nærmestelederFnr) {
        return Map.of("910825526", 4);
    }
}
