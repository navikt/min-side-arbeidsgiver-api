package no.nav.arbeidsgiver.min_side.tilgangssoknad

import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AltinnDelegationRequestsDeserializeTest {

    @Test
    fun deserializeDelegationRequestResponse() {
        //language=JSON
        val json = """
            {
              "id": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
              "status": "Pending",
              "type": "resource",
              "lastUpdated": "2025-01-01T00:00:00Z",
              "resource": {
                "referenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
              },
              "links": {
                "statusLink": "https://altinn.no/status/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
              },
              "from": {
                "organizationIdentifier": "11111111111"
              },
              "to": {
                "organizationIdentifier": "987654321"
              }
            }
        """.trimIndent()

        val result = defaultJson.decodeFromString<DelegationRequestResponse>(json)
        assertNotNull(result.id)
        assertEquals(DelegationRequestStatus.Pending, result.status)
        assertNotNull(result.resource?.referenceId)
        assertNotNull(result.links?.statusLink)
        assertNotNull(result.from?.organizationIdentifier)
        assertNotNull(result.to?.organizationIdentifier)
    }

    @Test
    fun deserializeDelegationRequestStatus() {
        val json = "\"Approved\""
        val result = defaultJson.decodeFromString<DelegationRequestStatus>(json)
        assertEquals(DelegationRequestStatus.Approved, result)
    }

    @Test
    fun deserializeCreateDelegationRequest() {
        //language=JSON
        val json = """
            {
              "to": "urn:altinn:organization:identifier-no:987654321",
              "resource": {
                "referenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
              }
            }
        """.trimIndent()

        val result = defaultJson.decodeFromString<CreateDelegationRequest>(json)
        assertEquals("urn:altinn:organization:identifier-no:987654321", result.to)
        assertNotNull(result.resource?.referenceId)
    }
}