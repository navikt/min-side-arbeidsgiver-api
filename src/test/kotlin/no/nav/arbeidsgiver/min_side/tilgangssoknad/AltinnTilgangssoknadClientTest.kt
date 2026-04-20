package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.infrastruktur.AltinnPlattformTokenClient
import no.nav.arbeidsgiver.min_side.infrastruktur.Miljø
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import org.skyscreamer.jsonassert.JSONAssert
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AltinnTilgangssoknadClientTest {

    private val fakePlatformTokenClient = object : AltinnPlattformTokenClient {
        override suspend fun token(scope: String): String = "platform-token-for-$scope"
    }

    @Test
    fun opprettDelegationRequest() {
        val capturedRequestBody = AtomicReference<String>()
        runTestApplication(
            externalServicesCfg = {
                hosts(Miljø.Altinn.platformBaseUrl) {
                    install(ContentNegotiation) {
                        json(defaultJson)
                    }
                    routing {
                        post("/accessmanagement/api/v1/serviceowner/delegationrequests") {
                            assertEquals(
                                "Bearer platform-token-for-altinn:serviceowner/delegationrequests.write",
                                call.request.header("Authorization")
                            )
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
                                    "organizationIdentifier": "987654321"
                                  },
                                  "to": {
                                    "personIdentifier": "11111111111"
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
                provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
                provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
            },
        ) {
            val request = CreateDelegationRequest(
                to = "urn:altinn:organization:identifier-no:987654321",
                resource = RequestReferenceDto(
                    referenceId = "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger"
                ),
            )

            val result = resolve<AltinnTilgangssoknadClient>().opprettDelegationRequest("11111111111", request)
            assertEquals("1a9e3a32-252b-4d81-a23c-ed0d86b852c7", result.id)
            assertEquals(DelegationRequestStatus.Pending, result.status)
            assertNotNull(result.links?.statusLink)

            JSONAssert.assertEquals(
                //language=JSON
                """
                {
                  "from": "urn:altinn:organization:identifier-no:987654321",
                  "to": "urn:altinn:person:identifier-no:11111111111",
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
            hosts(Miljø.Altinn.platformBaseUrl) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
                routing {
                    get("/accessmanagement/api/v1/serviceowner/delegationrequests/{id}/status") {
                        assertEquals(
                            "Bearer platform-token-for-altinn:serviceowner/delegationrequests.read",
                            call.request.header("Authorization")
                        )
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
            provide<AltinnPlattformTokenClient> { fakePlatformTokenClient }
            provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
        },
    ) {
        val result = resolve<AltinnTilgangssoknadClient>()
            .hentDelegationRequestStatus("1a9e3a32-252b-4d81-a23c-ed0d86b852c7")
        assertEquals(DelegationRequestStatus.Approved, result)
    }

}
