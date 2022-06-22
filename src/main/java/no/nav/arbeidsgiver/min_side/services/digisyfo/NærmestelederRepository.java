package no.nav.arbeidsgiver.min_side.services.digisyfo;

import java.util.List;

public interface NærmestelederRepository {
    List<String> virksomheterSomNærmesteLeder(String lederFnr);
}
