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
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class AntlinnTilgangSoknadApiTest {

    @Test
    fun opprettDelegationRequest() = runTestApplication(
        externalServicesCfg = {
            hosts(AltinnTilgangerService.ingress) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
                routing {
                    post("/delegation-request") {
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
                                "statusLink": "https://altinn.no/status/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
                              },
                              "from": {
                                "organizationIdentifier": "11111111111"
                              },
                              "to": {
                                "organizationIdentifier": "987654321"
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
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
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
    }

    @Test
    fun hentDelegationRequestStatus() = runTestApplication(
        externalServicesCfg = {
            hosts(AltinnTilgangerService.ingress) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
                routing {
                    get("/delegation-request/{id}/status") {
                        assertEquals("1a9e3a32-252b-4d81-a23c-ed0d86b852c7", call.parameters["id"])
                        call.respondText(
                            "\"Approved\"",
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
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        val jsonResponse = client.get("ditt-nav-arbeidsgiver-api/api/delegation-request/1a9e3a32-252b-4d81-a23c-ed0d86b852c7/status") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            it.bodyAsText()
        }

        assertEquals("\"Approved\"", jsonResponse)
    }

    @Test
    fun uautentisertBrukerFår401() = runTestApplication(
        externalServicesCfg = {
            hosts(AltinnTilgangerService.ingress) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
                routing {
                    post("/delegation-request") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector { null }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
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
