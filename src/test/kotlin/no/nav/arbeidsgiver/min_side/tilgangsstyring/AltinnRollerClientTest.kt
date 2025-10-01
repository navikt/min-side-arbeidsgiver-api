package no.nav.arbeidsgiver.min_side.tilgangsstyring

import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension


class AltinnRollerClientTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AltinnRollerClient>(AltinnRollerClient::class)
                provide<MaskinportenTokenService> { MaskinportenTokenServiceStub() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    /* Ved ukjent fnr svarer altinn:
     *   HTTP/1.1 400 012345678912 is not a valid organization number or social security number.
     *
     * Ved ukjent orgnr svarer altinn:
     *   HTTP/1.1 400 012345678 is not a valid organization number or social security number.
     */

    @Test
    fun `ingen tilgang ved ingen roller`() = app.runTest {
        mockRoles("1234", "567812345", ingenRollerResponse)
        assertFalse(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567812345",
                altinnRoller = setOf("SIGNE"),
                externalRoller = setOf("DAGL")
            )
        )
    }

    @Test
    fun `tilgang hvis vi sjekker DAGL(ereg) og bruker er DAGL(ereg), ATTST(altinn)`() = app.runTest {
        mockRoles("1234", "567812345", daglOgAttstRolleResponse)
        assertTrue(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567812345",
                altinnRoller = setOf("SIGN"),
                externalRoller = setOf("DAGL"),
            )
        )
    }

    @Test
    fun `tilgang hvis vi sjekker HADM(altinn) og bruker er DAGL(ereg), HADM(altinn)`() = app.runTest {
        mockRoles("1234", "567812345", daglOgHadmRolleResponse)
        assertTrue(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567812345",
                altinnRoller = setOf("HADM"),
                externalRoller = setOf("ANNENROLLE"),
            )
        )
    }

    @Test
    fun `ikke tilgang hvis altinn- og ereg-roller byttes om`() = app.runTest {
        mockRoles("1234", "567811223", daglOgHadmRolleResponse)
        assertFalse(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567811223",
                altinnRoller = setOf("DAGL"),
                externalRoller = setOf("HADM"),
            )
        )
    }

    @Test
    fun `har tilgang hvis man både har ereg- og altinn-rolle`() = app.runTest {
        mockRoles("1234", "567811223", daglOgAttstRolleResponse)
        assertTrue(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567811223",
                altinnRoller = setOf("ATTST"),
                externalRoller = setOf("DAGL")
            )
        )
    }

    @Test
    fun `bruker trenger ikke å ha alle rollene vi spør om`() = app.runTest {
        mockRoles("1234", "567811223", daglOgAttstRolleResponse)
        assertTrue(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567811223",
                altinnRoller = setOf("ATTST"),
                externalRoller = setOf("DAGL", "ANNENROLLE"),
            )
        )
    }

    @Test
    fun `ikke tilgang selv med flere roller og rolle-sjekker`() = app.runTest {
        mockRoles("789", "567811223", daglOgAttstRolleResponse)
        assertFalse(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "789",
                orgnr = "567811223",
                altinnRoller = setOf("IKKEROLLE"),
                externalRoller = setOf("ANNENIKKEROLLE"),
            )
        )
    }

    @Test
    fun `tolker ikke Local-roller som ereg-roller`() = app.runTest {
        mockRoles("1234", "567811223", daglMenLocalRolleResponse)
        assertFalse(
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567811223",
                altinnRoller = setOf("ANNEN"),
                externalRoller = setOf("DAGL"),
            )
        )
    }


    @Test
    fun `exception hvis ingen roller oppgis`() = app.runTest {
        mockRoles("1234", "567811223", daglOgAttstRolleResponse)
        assertThrows<IllegalArgumentException> {
            app.getDependency<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567811223",
                altinnRoller = setOf(),
                externalRoller = setOf()
            )
        }
    }

    private fun mockRoles(fnr: String, orgnr: String, response: String) =
        fakeApi.registerStub(
            HttpMethod.Get,
            "/api/serviceowner/authorization/roles"
        ) {
            val queryParams = call.request.queryParameters
            assertTrue("ForceEIAuthentication" in queryParams)
            assertTrue("\$filter" in queryParams)
            assertEquals(fnr, queryParams["subject"])
            assertEquals(orgnr, queryParams["reportee"])

            call.response.headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            call.respond(response)
        }
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
