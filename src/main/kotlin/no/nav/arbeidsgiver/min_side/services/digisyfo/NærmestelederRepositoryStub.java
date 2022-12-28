package no.nav.arbeidsgiver.min_side.services.digisyfo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"local", "labs"})
@Repository
public class NærmestelederRepositoryStub implements NærmestelederRepository {
    @Override
    public List<String> virksomheterSomNærmesteLeder(String lederFnr) {
        return List.of("910825526");
    }
}
