package no.nav.arbeidsgiver.min_side.services.tiltak;

import lombok.Data;

import java.util.List;
import java.util.Map;

public interface RefusjonStatusRepository {

    @Data
    class Statusoversikt {
        public final String virksomhetsnummer;

        /**
         * Antall refusjoner per status. Kan være tomt map. Hvis det ikke er noen saker med gitt status,
         *          så er ikke nødvendigvis statusen i mappet.
         */
        public final Map<String, Integer> statusoversikt;
    }

    List<Statusoversikt> statusoversikt(List<String> virksomhetsnummer);
}
