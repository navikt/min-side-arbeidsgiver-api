package no.nav.arbeidsgiver.min_side.clients.altinn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.clients.altinn.dto.Søknadsstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [JacksonAutoConfiguration::class])
class AltinnDelegationRequestsDeserializeTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun deserialiseJsonResponseIntoDTO() {
        val text = this.javaClass.getResourceAsStream("/altinnTilgangerGet.json")!!.readAllBytes()
        val readValue = objectMapper.readValue<Søknadsstatus>(text)
        assertThat(readValue.embedded).isNotNull
        assertThat(readValue.embedded!!.delegationRequests).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].CoveredBy).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].Created).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].KeepSessionAlive).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].LastChanged).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].links).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].links!!.sendRequest).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].links!!.sendRequest!!.href).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].OfferedBy).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].RequestStatus).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].RedirectUrl).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].RequestResources).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].RequestResources!![0].ServiceCode).isNotNull
        assertThat(readValue.embedded!!.delegationRequests!![0].RequestResources!![0].ServiceEditionCode).isNotNull
        assertThat(readValue.continuationtoken).isNotNull

        /* no parse error */
    }
}