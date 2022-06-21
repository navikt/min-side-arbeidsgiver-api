package no.nav.arbeidsgiver.min_side.services.tiltak;

import java.util.Map;

public interface RefusjonStatusRepository {
    /**
     * @param virksomhetsummer
     * @return Antall refusjoner per status. Kan være tomt map. Hvis det ikke er noen saker med gitt status,
     *          så er ikke nødvendigvis statusen i mappet.
     */
    Map<String, Integer> statusoversikt(String virksomhetsummer);
}
