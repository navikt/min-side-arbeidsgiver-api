package no.nav.arbeidsgiver.min_side.tilgangsstyring

import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.lang.IllegalArgumentException


@RestClientTest(
    AltinnRollerClient::class,
    MaskinportenTokenServiceStub::class,
)
@ActiveProfiles("local")
class AltinnRollerClientTest {
    @Autowired
    lateinit var altinnServer: MockRestServiceServer

    @Autowired
    lateinit var altinnRollerClient: AltinnRollerClient

    /* Ved ukjent fnr svarer altinn:
     *   HTTP/1.1 400 012345678912 is not a valid organization number or social security number.
     *
     * Ved ukjent orgnr svarer altinn:
     *   HTTP/1.1 400 012345678 is not a valid organization number or social security number.
     */

    @Test
    fun `ingen tilgang ved ingen roller`() {
        mockRoles("1234", "5678", ingenRollerResponse)
        assertFalse(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "5678",
            altinnRoller = setOf("SIGNE"),
            externalRoller = setOf("DAGL")
        ))
    }

    @Test
    fun `tilgang hvis vi sjekker DAGL(ereg) og bruker er DAGL(ereg), ATTST(altinn)`() {
        mockRoles("1234", "5678", daglOgAttstRolleResponse)
        assertTrue(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "5678",
            altinnRoller = setOf("SIGN"),
            externalRoller = setOf("DAGL"),
        ))
    }

    @Test
    fun `tilgang hvis vi sjekker HADM(altinn) og bruker er DAGL(ereg), HADM(altinn)`() {
        mockRoles("1234", "5678", daglOgHadmRolleResponse)
        assertTrue(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "5678",
            altinnRoller = setOf("HADM"),
            externalRoller = setOf("ANNENROLLE"),
        ))
    }

    @Test
    fun `ikke tilgang hvis altinn- og ereg-roller byttes om`() {
        mockRoles("1234", "5678", daglOgHadmRolleResponse)
        assertFalse(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "5678",
            altinnRoller = setOf("DAGL"),
            externalRoller = setOf("HADM"),
        ))
    }

    @Test
    fun `har tilgang hvis man både har ereg- og altinn-rolle`() {
        mockRoles("1234", "5678", daglOgAttstRolleResponse)
        assertTrue(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "5678",
            altinnRoller = setOf("ATTST"),
            externalRoller = setOf("DAGL")
        ))
    }

    @Test
    fun `bruker trenger ikke å ha alle rollene vi spør om`() {
        mockRoles("1234", "5678", daglOgAttstRolleResponse)
        assertTrue(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "5678",
            altinnRoller = setOf("ATTST"),
            externalRoller = setOf("DAGL", "ANNENROLLE"),
        ))
    }

    @Test
    fun `ikke tilgang selv med flere roller og rolle-sjekker`() {
        mockRoles("789", "5678", daglOgAttstRolleResponse)
        assertFalse(altinnRollerClient.harAltinnRolle(
            fnr = "789",
            orgnr = "5678",
            altinnRoller = setOf("IKKEROLLE"),
            externalRoller = setOf("ANNENIKKEROLLE"),
        ))
    }

    @Test
    fun `tolker ikke Local-roller som ereg-roller`() {
        mockRoles("1234", "567", daglMenLocalRolleResponse)
        assertFalse(altinnRollerClient.harAltinnRolle(
            fnr = "1234",
            orgnr = "567",
            altinnRoller = setOf("ANNEN"),
            externalRoller = setOf("DAGL"),
        ))
    }


    @Test
    fun `exception hvis ingen roller oppgis`() {
        mockRoles("1234", "5678", daglOgAttstRolleResponse)
        assertThrows<IllegalArgumentException> {
            altinnRollerClient.harAltinnRolle(
                fnr = "1234",
                orgnr = "5678",
                altinnRoller = setOf(),
                externalRoller = setOf()
            )
        }
    }

    private fun mockRoles(fnr: String, orgnr: String, response: String) =
        altinnServer.expect {
            assertEquals("/api/serviceowner/authorization/roles", it.uri.path)
            assertNotNull(it.uri.query)
            val queryParams = it.uri.query.removePrefix("?").split("&")
                .map { it.split("=") }
                .associate { it.get(0) to it.getOrNull(1) }
            assertTrue("ForceEIAuthentication" in queryParams)
            assertTrue("\$filter" in queryParams)
            assertEquals(fnr, queryParams["subject"])
            assertEquals(orgnr, queryParams["reportee"])

        }.andRespond(withSuccess(response, MediaType.APPLICATION_JSON))
}

/* Har ikke fått til få tt02 til å returnere tom liste. Men høres ikke utenkelig ut at det er mulig. */
private val ingenRollerResponse = """
    []
"""

/* Hentet fra tt02.altinn.no */
private val daglOgAttstRolleResponse = """
    [
      {
        "RoleType": "Altinn",
        "RoleDefinitionId": 85,
        "RoleName": "Auditor certifies validity of VAT compensation",
        "RoleDescription": "Certification by auditor of RF-0009",
        "RoleDefinitionCode": "ATTST",
        "_links": [
          {
            "Rel": "roledefinition",
            "Href": "https://tt02.altinn.no/api/serviceowner/roledefinitions/85",
            "Title": null,
            "FileNameWithExtension": null,
            "MimeType": null,
            "IsTemplated": false,
            "Encrypted": false,
            "SigningLocked": false,
            "SignedByDefault": false,
            "FileSize": 0
          }
        ]
      },
      {
        "RoleId": 45084,
        "RoleType": "External",
        "RoleDefinitionId": 195,
        "RoleName": "General manager",
        "RoleDescription": "External role (from The Central Coordinating Register for Legal Entities)",
        "RoleDefinitionCode": "DAGL",
        "_links": [
          {
            "Rel": "roledefinition",
            "Href": "https://tt02.altinn.no/api/serviceowner/roledefinitions/195",
            "Title": null,
            "FileNameWithExtension": null,
            "MimeType": null,
            "IsTemplated": false,
            "Encrypted": false,
            "SigningLocked": false,
            "SignedByDefault": false,
            "FileSize": 0
          }
        ]
      }
    ]
""".trimIndent()

/* modifisert svar fra tt02.altinn.no */
private val daglOgHadmRolleResponse = """
    [
      {
        "RoleType": "Altinn",
        "RoleDefinitionId": 85,
        "RoleName": "Auditor certifies validity of VAT compensation",
        "RoleDescription": "Certification by auditor of RF-0009",
        "RoleDefinitionCode": "HADM",
        "_links": [
          {
            "Rel": "roledefinition",
            "Href": "https://tt02.altinn.no/api/serviceowner/roledefinitions/85",
            "Title": null,
            "FileNameWithExtension": null,
            "MimeType": null,
            "IsTemplated": false,
            "Encrypted": false,
            "SigningLocked": false,
            "SignedByDefault": false,
            "FileSize": 0
          }
        ]
      },
      {
        "RoleId": 45084,
        "RoleType": "External",
        "RoleDefinitionId": 195,
        "RoleName": "General manager",
        "RoleDescription": "External role (from The Central Coordinating Register for Legal Entities)",
        "RoleDefinitionCode": "DAGL",
        "_links": [
          {
            "Rel": "roledefinition",
            "Href": "https://tt02.altinn.no/api/serviceowner/roledefinitions/195",
            "Title": null,
            "FileNameWithExtension": null,
            "MimeType": null,
            "IsTemplated": false,
            "Encrypted": false,
            "SigningLocked": false,
            "SignedByDefault": false,
            "FileSize": 0
          }
        ]
      }
    ]
""".trimIndent()

/* Svar fra tt02.altinn.no */
private val daglMenLocalRolleResponse = """
    [
      {
        "RoleType": "Local",
        "RoleDefinitionId": 0,
        "RoleName": "Single Rights",
        "RoleDescription": "Collection of single rights",
        "Delegator": "XXX",
        "DelegatedTime": "2012-12-03T10:39:59.233",
        "RoleDefinitionCode": "DAGL",
        "_links": [
          {
            "Rel": "roledefinition",
            "Href": "https://tt02.altinn.no/api/serviceowner/roledefinitions/0",
            "Title": null,
            "FileNameWithExtension": null,
            "MimeType": null,
            "IsTemplated": false,
            "Encrypted": false,
            "SigningLocked": false,
            "SignedByDefault": false,
            "FileSize": 0
          }
        ]
      }
    ]
"""
