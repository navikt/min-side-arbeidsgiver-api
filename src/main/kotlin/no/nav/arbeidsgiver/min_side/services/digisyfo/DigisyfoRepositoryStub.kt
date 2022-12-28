package no.nav.arbeidsgiver.min_side.services.digisyfo;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"local", "labs"})
@Repository
public class DigisyfoRepositoryStub implements DigisyfoRepository {
    @Override
    public List<Virksomhetsinfo> virksomheterOgSykmeldte(String nærmestelederFnr) {
        return List.of(new Virksomhetsinfo("910825526", 4));
    }
    @Override
    public void processNærmesteLederEvent(NarmesteLederHendelse hendelse) {
    }

    @Override
    public void processSykmeldingEvent(List<ImmutablePair<String, SykmeldingHendelse>> records) {
    }
}
