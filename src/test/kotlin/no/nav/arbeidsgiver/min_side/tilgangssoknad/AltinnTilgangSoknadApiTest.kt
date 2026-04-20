package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.configureTilgangssoknadRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class AltinnTilgangSoknadApiTest {

    private val fakePlatformTokenClient = object : AltinnPlattformTokenClient {
        override suspend fun token(scope: String): String = "platform-token-for-$scope"
    }

    @Test
    fun `opprett delegation request lagrer i database og returnerer 202`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            hosts(Miljø.Altinn.platformBaseUrl) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
                routing {
                    post("/accessmanagement/api/v1/serviceowner/delegationrequests") {
                        call.respondText(
                            //language=JSON
                            """
                            {
                              "id": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                              "status": "Pending",
                              "type": "resource",
                              "lastUpdated": "2025-01-01T00:00:00Z",
                              "resource": {
                                "referenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
                              },
                              "links": {
                                "detailsLink": "https://altinn.no/details/1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                                "statusLink": "https://altinn.no/status/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
                              },
                              "from": {
                                "organizationIdentifier": "987654321"
                              },
                              "to": {
                                "personIdentifier": "42"
                              }
                            }
                            """.trimIndent(),
                            ContentType.Application.Json
                        )
                    }
                }
            }
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
            provide<DelegationRequestRepository>(DelegationRequestRepository::class)
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        val jsonResponse = client.post("ditt-nav-arbeidsgiver-api/api/delegation-request") {
            contentType(ContentType.Application.Json)
            bearerAuth("faketoken")
            setBody(
                """
                {
                    "to": "urn:altinn:organization:identifier-no:987654321",
                    "resource": {
                        "referenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
                    }
                }
                """
            )
        }.let {
            assertEquals(HttpStatusCode.Accepted, it.status)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            {
              "id": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
              "status": "Pending",
              "resource": {
                "referenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
              }
            }
            """, jsonResponse, false
        )

        // persisted row is returned from the list endpoint
        val listJson = client.get("ditt-nav-arbeidsgiver-api/api/delegation-request") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            [
              {
                "id": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                "orgnr": "987654321",
                "resourceReferenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger",
                "status": "Pending",
                "detailsLink": "https://altinn.no/details/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
              }
            ]
            """, listJson, false
        )
    }

    @Test
    fun `mine delegation requests returnerer kun brukerens egne`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
            provide<AltinnTilgangssoknadClient> {
                object : AltinnTilgangssoknadClient {
                    override suspend fun opprettDelegationRequest(
                        fnr: String,
                        request: CreateDelegationRequest,
                    ) = error("not used")
                    override suspend fun hentDelegationRequestStatus(id: String) = error("not used")
                }
            }
            provide<DelegationRequestRepository>(DelegationRequestRepository::class)
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        val repo = resolve<DelegationRequestRepository>()
        // Terminal status → no Altinn refresh needed
        repo.lagre(
            id = java.util.UUID.fromString("1a9e3a32-252b-4d81-a23c-ed0d86b852c7"),
            fnr = "42",
            orgnr = "987654321",
            resourceReferenceId = "nav_resource_a",
            status = "Approved",
            detailsLink = "https://altinn.no/details/a",
            lastResponseJson = """{"id":"1a9e3a32-252b-4d81-a23c-ed0d86b852c7","status":"Approved"}""",
        )
        repo.lagre(
            id = java.util.UUID.fromString("2b9e3a32-252b-4d81-a23c-ed0d86b852c7"),
            fnr = "99",
            orgnr = "987654321",
            resourceReferenceId = "nav_resource_b",
            status = "Approved",
            detailsLink = null,
            lastResponseJson = """{"id":"2b9e3a32-252b-4d81-a23c-ed0d86b852c7","status":"Approved"}""",
        )

        val listJson = client.get("ditt-nav-arbeidsgiver-api/api/delegation-request") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            [
              {
                "id": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                "orgnr": "987654321",
                "resourceReferenceId": "nav_resource_a",
                "status": "Approved"
              }
            ]
            """, listJson, false
        )
    }

    @Test
    fun `mine delegation requests refresher ikke-terminal status fra altinn`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            hosts(Miljø.Altinn.platformBaseUrl) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
                routing {
                    get("/accessmanagement/api/v1/serviceowner/delegationrequests/{id}/status") {
                        call.respondText("\"Approved\"", ContentType.Application.Json)
                    }
                }
            }
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
            provide<DelegationRequestRepository>(DelegationRequestRepository::class)
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        val repo = resolve<DelegationRequestRepository>()
        val id = java.util.UUID.fromString("1a9e3a32-252b-4d81-a23c-ed0d86b852c7")
        repo.lagre(
            id = id,
            fnr = "42",
            orgnr = "987654321",
            resourceReferenceId = "nav_resource_a",
            status = "Pending",
            detailsLink = "https://altinn.no/details/a",
            lastResponseJson = """{"id":"1a9e3a32-252b-4d81-a23c-ed0d86b852c7","status":"Pending"}""",
        )

        val listJson = client.get("ditt-nav-arbeidsgiver-api/api/delegation-request") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            [
              { "id": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7", "status": "Approved" }
            ]
            """, listJson, false
        )

        // persisted row was updated
        assertEquals("Approved", repo.hentForBruker("42").single().status)
    }

    @Test
    fun `opprett avvises med 400 hvis resource referenceId mangler nav_ prefiks`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
            provide<AltinnTilgangssoknadClient> {
                object : AltinnTilgangssoknadClient {
                    override suspend fun opprettDelegationRequest(
                        fnr: String,
                        request: CreateDelegationRequest,
                    ) = error("skal ikke kalles ved ugyldig resource")
                    override suspend fun hentDelegationRequestStatus(id: String) = error("not used")
                }
            }
            provide<DelegationRequestRepository>(DelegationRequestRepository::class)
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        client.post("ditt-nav-arbeidsgiver-api/api/delegation-request") {
            contentType(ContentType.Application.Json)
            bearerAuth("faketoken")
            setBody(
                """
                {
                    "to": "urn:altinn:organization:identifier-no:987654321",
                    "resource": { "referenceId": "not_a_nav_resource" }
                }
                """
            )
        }.let {
            assertEquals(HttpStatusCode.BadRequest, it.status)
        }
    }

    @Test
    fun `uautentisert bruker fr 401`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector { null }
            }
            provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
            provide<DelegationRequestRepository>(DelegationRequestRepository::class)
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        client.post("ditt-nav-arbeidsgiver-api/api/delegation-request") {
            contentType(ContentType.Application.Json)
            bearerAuth("invalidtoken")
            setBody(
                """
                {
                    "to": "urn:altinn:organization:identifier-no:987654321",
                    "resource": {
                        "referenceId": "nav_some_resource"
                    }
                }
                """
            )
        }.let {
            assertEquals(HttpStatusCode.Unauthorized, it.status)
        }
    }
}
