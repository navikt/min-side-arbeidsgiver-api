package no.nav.arbeidsgiver.min_side.tilgangssoknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.defaultConfiguration
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class AltinnDelegationRequestsDeserializeTest {

    val objectMapper = ObjectMapper().defaultConfiguration()
    @Test
    fun deserialiseJsonResponseIntoDTO()  {
        val text = this.javaClass.getResourceAsStream("/altinnTilgangerGet.json")!!.readAllBytes()
        val readValue = objectMapper.readValue<Søknadsstatus>(text)
        Assertions.assertThat(readValue.embedded).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].CoveredBy).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].Created).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].KeepSessionAlive).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].LastChanged).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].links).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].links!!.sendRequest).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].links!!.sendRequest!!.href).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].OfferedBy).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].RequestStatus).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].RedirectUrl).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].RequestResources).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].RequestResources!![0].ServiceCode).isNotNull
        Assertions.assertThat(readValue.embedded!!.delegationRequests!![0].RequestResources!![0].ServiceEditionCode).isNotNull
        Assertions.assertThat(readValue.continuationtoken).isNotNull

        /* no parse error */
    }
}