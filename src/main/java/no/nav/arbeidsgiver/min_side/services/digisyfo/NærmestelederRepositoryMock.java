package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile({"local", "labs"})
@Slf4j
@Repository
public class NærmestelederRepositoryMock implements NærmestelederRepository {
    @Override
    public boolean erNærmesteLederForNoen(String lederFnr) {
        return true;
    }
}
