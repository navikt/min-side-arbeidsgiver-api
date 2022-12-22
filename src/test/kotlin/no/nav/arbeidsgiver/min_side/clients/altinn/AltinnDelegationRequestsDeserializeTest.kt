package no.nav.arbeidsgiver.min_side.clients.altinn

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.Søknadsstatus
import org.junit.Test

class AltinnDelegationRequestsDeserializeTest {

    @Test
    fun deserialiseJsonResponseIntoDTO() {
        val text = this.javaClass.getResourceAsStream("/altinnTilgangerGet.json")!!.readAllBytes()
        val mapper = JsonMapper()
        mapper.readValue<Søknadsstatus>(text)

        /* no parse error */
    }
}