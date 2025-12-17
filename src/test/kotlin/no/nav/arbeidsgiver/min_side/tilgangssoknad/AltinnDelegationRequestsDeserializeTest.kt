package no.nav.arbeidsgiver.min_side.tilgangssoknad

import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import kotlin.test.Test
import kotlin.test.assertNotNull

class AltinnDelegationRequestsDeserializeTest {

    @Test
    fun deserialiseJsonResponseIntoDTO()  {
        val text = requireNotNull(javaClass.getResource("/altinnTilgangerGet.json")).readText()
        val readValue = defaultJson.decodeFromString<Søknadsstatus>(text)
        assertNotNull(readValue.embedded)
        assertNotNull(readValue.embedded.delegationRequests)
        assertNotNull(readValue.embedded.delegationRequests[0].CoveredBy)
        assertNotNull(readValue.embedded.delegationRequests[0].Created)
        assertNotNull(readValue.embedded.delegationRequests[0].KeepSessionAlive)
        assertNotNull(readValue.embedded.delegationRequests[0].LastChanged)
        assertNotNull(readValue.embedded.delegationRequests[0].links)
        assertNotNull(readValue.embedded.delegationRequests[0].links!!.sendRequest)
        assertNotNull(readValue.embedded.delegationRequests[0].links!!.sendRequest!!.href)
        assertNotNull(readValue.embedded.delegationRequests[0].OfferedBy)
        assertNotNull(readValue.embedded.delegationRequests[0].RequestStatus)
        assertNotNull(readValue.embedded.delegationRequests[0].RedirectUrl)
        assertNotNull(readValue.embedded.delegationRequests[0].RequestResources)
        assertNotNull(readValue.embedded.delegationRequests[0].RequestResources!![0].ServiceCode)
        assertNotNull(readValue.embedded.delegationRequests[0].RequestResources!![0].ServiceEditionCode)
        assertNotNull(readValue.continuationtoken)

        /* no parse error */
    }
}