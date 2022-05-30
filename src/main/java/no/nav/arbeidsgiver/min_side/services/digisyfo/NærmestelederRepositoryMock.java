package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"local", "labs"})
@Slf4j
@Repository
public class NærmestelederRepositoryMock implements NærmestelederRepository {
    @Override
    public List<String> virksomheterSomNærmesteLeder(String lederFnr) {
        return List.of("910825526");
    }
}
