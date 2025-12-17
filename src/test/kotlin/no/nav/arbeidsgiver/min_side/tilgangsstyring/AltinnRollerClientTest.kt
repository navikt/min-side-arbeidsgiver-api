package no.nav.arbeidsgiver.min_side.tilgangsstyring

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*


class AltinnRollerClientTest {
    /* Ved ukjent fnr svarer altinn:
     *   HTTP/1.1 400 012345678912 is not a valid organization number or social security number.
     *
     * Ved ukjent orgnr svarer altinn:
     *   HTTP/1.1 400 012345678 is not a valid organization number or social security number.
     */

    @Test
    fun `ingen tilgang ved ingen roller`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)
                            call.respondText(ingenRollerResponse, ContentType.Application.Json)
                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertFalse(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567812345",
                    altinnRoller = setOf("SIGNE"),
                    externalRoller = setOf("DAGL")
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567812345", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `tilgang hvis vi sjekker DAGL(ereg) og bruker er DAGL(ereg), ATTST(altinn)`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)
                            call.respondText(daglOgAttstRolleResponse, ContentType.Application.Json)

                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertTrue(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567812345",
                    altinnRoller = setOf("SIGN"),
                    externalRoller = setOf("DAGL"),
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567812345", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `tilgang hvis vi sjekker HADM(altinn) og bruker er DAGL(ereg), HADM(altinn)`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)

                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                            call.respondText(daglOgHadmRolleResponse, ContentType.Application.Json)

                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertTrue(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567812345",
                    altinnRoller = setOf("HADM"),
                    externalRoller = setOf("ANNENROLLE"),
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567812345", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `ikke tilgang hvis altinn- og ereg-roller byttes om`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)

                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                            call.respondText(daglOgHadmRolleResponse, ContentType.Application.Json)
                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertFalse(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567811223",
                    altinnRoller = setOf("DAGL"),
                    externalRoller = setOf("HADM"),
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567811223", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `har tilgang hvis man både har ereg- og altinn-rolle`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)

                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                            call.respondText(daglOgAttstRolleResponse, ContentType.Application.Json)
                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertTrue(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567811223",
                    altinnRoller = setOf("ATTST"),
                    externalRoller = setOf("DAGL")
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567811223", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `bruker trenger ikke å ha alle rollene vi spør om`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)

                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                            call.respondText(daglOgAttstRolleResponse, ContentType.Application.Json)
                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertTrue(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567811223",
                    altinnRoller = setOf("ATTST"),
                    externalRoller = setOf("DAGL", "ANNENROLLE"),
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567811223", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `ikke tilgang selv med flere roller og rolle-sjekker`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)

                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                            call.respondText(daglOgAttstRolleResponse, ContentType.Application.Json)
                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertFalse(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "789",
                    orgnr = "567811223",
                    altinnRoller = setOf("IKKEROLLE"),
                    externalRoller = setOf("ANNENIKKEROLLE"),
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("789", queryParams["subject"])
                assertEquals("567811223", queryParams["reportee"])
            }
        }
    }

    @Test
    fun `tolker ikke Local-roller som ereg-roller`() {
        val queryParameters = AtomicReference<Parameters>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnRollerClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            json(defaultJson)
                        }

                        get(AltinnRollerClient.apiPath) {
                            queryParameters.set(call.request.queryParameters)

                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                            call.respondText(daglMenLocalRolleResponse, ContentType.Application.Json)
                        }
                    }
                }
            },
            dependenciesCfg = {
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
            }
        ) {
            assertFalse(
                resolve<AltinnRollerClient>().harAltinnRolle(
                    fnr = "1234",
                    orgnr = "567811223",
                    altinnRoller = setOf("ANNEN"),
                    externalRoller = setOf("DAGL"),
                )
            )
            queryParameters.get().let { queryParams ->
                assertTrue($$"$filter" in queryParams)
                assertEquals("1234", queryParams["subject"])
                assertEquals("567811223", queryParams["reportee"])
            }
        }
    }


    @Test
    fun `exception hvis ingen roller oppgis`() = runTestApplication(
        dependenciesCfg = {
            provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
            provide<AltinnRollerClient>(AltinnRollerClientImpl::class)
        }
    ) {
        assertFailsWith<IllegalArgumentException> {
            resolve<AltinnRollerClient>().harAltinnRolle(
                fnr = "1234",
                orgnr = "567811223",
                altinnRoller = setOf(),
                externalRoller = setOf()
            )
        }
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
