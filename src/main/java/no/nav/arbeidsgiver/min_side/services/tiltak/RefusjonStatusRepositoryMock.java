package no.nav.arbeidsgiver.min_side.services.tiltak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Profile({"local", "labs"})
@Slf4j
@Repository
public class RefusjonStatusRepositoryMock implements RefusjonStatusRepository {
    @Override
    public List<Statusoversikt> statusoversikt(List<String> virksomhetsnummer) {
        return List.of();
    }
}
