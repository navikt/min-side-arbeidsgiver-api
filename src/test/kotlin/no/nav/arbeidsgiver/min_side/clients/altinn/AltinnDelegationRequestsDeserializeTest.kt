package no.nav.arbeidsgiver.min_side.clients.altinn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.Søknadsstatus;
import org.junit.Test;

public class AltinnDelegationRequestsDeserializeTest {


    @SuppressWarnings({"resource", "ConstantConditions"})
    @Test
    public void deserialiseJsonResponseIntoDTO() throws Exception {
        var text = this.getClass().getResourceAsStream("/altinnTilgangerGet.json").readAllBytes();
        var mapper = new JsonMapper();
        var ignored = mapper.readValue(text, new TypeReference<Søknadsstatus>() {
        });

        /* no parse error */
    }
}
