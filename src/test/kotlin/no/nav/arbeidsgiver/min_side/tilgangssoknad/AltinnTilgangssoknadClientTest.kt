package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenResponse
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import org.skyscreamer.jsonassert.JSONAssert
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AltinnTilgangssoknadClientTest {

    private val testTokenExchanger = object : TokenXTokenExchanger {
        override suspend fun exchange(target: String, userToken: String) =
            TokenResponse.Success("$userToken-exchanged", 3600)
    }

    @Test
    fun opprettDelegationRequest() {
        val capturedRequestBody = AtomicReference<String>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnTilgangerService.ingress) {
                    install(ContentNegotiation) {
                        json(defaultJson)
                    }
                    routing {
                        post("/delegation-request") {
                            assertEquals("Bearer user-token-exchanged", call.request.header("Authorization"))
                            capturedRequestBody.set(call.receiveText())
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
                provide<TokenXTokenExchanger> { testTokenExchanger }
                provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
            },
        ) {
            val request = CreateDelegationRequest(
                to = "urn:altinn:organization:identifier-no:987654321",
                resource = RequestReferenceDto(
                    referenceId = "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
                ),
            )

            val result = resolve<AltinnTilgangssoknadClient>().opprettDelegationRequest("user-token", request)
            assertEquals("1a9e3a32-252b-4d81-a23c-ed0d86b852c7", result.id)
            assertEquals(DelegationRequestStatus.Pending, result.status)
            assertNotNull(result.links?.statusLink)

            JSONAssert.assertEquals(
                //language=JSON
                """
                {
                  "to": "urn:altinn:organization:identifier-no:987654321",
                  "resource": {
                    "referenceId": "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
                  }
                }
                """.trimIndent(),
                capturedRequestBody.get(),
                false
            )
        }
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
                        assertEquals("Bearer user-token-exchanged", call.request.header("Authorization"))
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
            provide<TokenXTokenExchanger> { testTokenExchanger }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
        },
    ) {
        val result = resolve<AltinnTilgangssoknadClient>()
            .hentDelegationRequestStatus("user-token", "1a9e3a32-252b-4d81-a23c-ed0d86b852c7")
        assertEquals(DelegationRequestStatus.Approved, result)
    }
}