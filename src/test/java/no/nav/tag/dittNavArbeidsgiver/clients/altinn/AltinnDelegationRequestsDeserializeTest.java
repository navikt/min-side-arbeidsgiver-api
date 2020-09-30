package no.nav.tag.dittNavArbeidsgiver.clients.altinn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.nav.tag.dittNavArbeidsgiver.clients.altinn.dto.Søknadsstatus;
import org.junit.Test;

import java.util.List;

public class AltinnDelegationRequestsDeserializeTest {


    @Test
    public void deserialiseJsonResponseIntoDTO() throws Exception {
        var text = this.getClass().getResourceAsStream("/altinnTilgangerGet.json").readAllBytes();
        var mapper = new JsonMapper();
        var json = mapper.readValue(text, new TypeReference<Søknadsstatus>() {
        });

        /* no parse error */
    }
}
