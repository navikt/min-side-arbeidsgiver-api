package no.nav.arbeidsgiver.min_side.services.tiltak;

import java.util.List;
import java.util.Map;

public interface RefusjonStatusRepository {

    class Statusoversikt {
        public String virksomhetsnummer;

        /**
         * Antall refusjoner per status. Kan være tomt map. Hvis det ikke er noen saker med gitt status,
         *          så er ikke nødvendigvis statusen i mappet.
         */
        public Map<String, Integer> statusoversikt;
    }

    List<Statusoversikt> statusoversikt(List<String> virksomhetsnummer);
}
