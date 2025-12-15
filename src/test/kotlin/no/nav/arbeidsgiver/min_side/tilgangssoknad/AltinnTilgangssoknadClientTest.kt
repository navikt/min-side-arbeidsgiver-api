package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.infrastruktur.MaskinportenTokenProvider
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.infrastruktur.successMaskinportenTokenProvider
import org.skyscreamer.jsonassert.JSONAssert
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertTrue

class AltinnTilgangssoknadClientTest {
    val fnr = "42"
    val orgnr = "314159265"

    /**
     * se: https://www.altinn.no/api/serviceowner/Help/Api/GET-serviceowner-delegationRequests_serviceCode_serviceEditionCode_status[0]_status[1]_continuation_coveredby_offeredby
     */
    @Test
    fun hentSøknader() = runTestApplication(
        externalServicesCfg = {
            hosts(AltinnTilgangssoknadClient.ingress) {
                routing {
                    install(ContentNegotiation) {
                        halJsonConfiguration()
                    }

                    get(AltinnTilgangssoknadClient.apiPath) {
                        if (call.request.header("accept")!!.contains("application/json")) {
                            // dersom application/json er med i listen vil Altinn ikke returnere HAL+JSON selv om den er med
                            // for unngå å måtte skrive om klienten nå, så returnerer vi feil her i testen og passer på at klienten
                            // ikke sender med application/json i accept-headeren
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "Feil i accept header: ${call.request.header("accept")!!}"
                            )
                        }

                        if (call.parameters[$$"$filter"] == "CoveredBy eq '$fnr'") {
                            call.respondText(
                                if (call.parameters["continuation"] == null)
                                    altinnHentSøknadResponse
                                else
                                    altinnHentSøknadTomResponse,
                                ContentType.Application.HalJson
                            )
                        }
                    }
                }
            }
        },
        dependenciesCfg = { builder ->
            provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
            provide<AltinnTilgangssoknadClient> {
                AltinnTilgangssoknadClientImpl(
                    tokenProvider = resolve(),
                    httpClient = builder.createClient {
                        halJsonHttpClientConfig()
                    }
                )
            }
        },
    ) {
        val result = resolve<AltinnTilgangssoknadClient>().hentSøknader(fnr)
        assertTrue(result.isNotEmpty())
    }

    /**
     * se https://www.altinn.no/api/serviceowner/Help/Api/POST-serviceowner-delegationRequests
     */
    @Test
    fun sendSøknad() {
        val capturedRequestBody = AtomicReference<String>()
        runTestApplication(
            externalServicesCfg = {
                hosts(AltinnTilgangssoknadClient.ingress) {
                    routing {
                        install(ContentNegotiation) {
                            halJsonConfiguration()
                        }

                        post(AltinnTilgangssoknadClient.apiPath) {
                            capturedRequestBody.set(call.receiveText())
                            call.response.header(HttpHeaders.ContentType, "application/hal+json")
                            call.respond(altinnSendSøknadResponse)
                        }
                    }
                }
            },
            dependenciesCfg = { builder ->
                provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
                provide<AltinnTilgangssoknadClient> {
                    AltinnTilgangssoknadClientImpl(
                        tokenProvider = resolve(),
                        httpClient = builder.createClient {
                            halJsonHttpClientConfig()
                        }
                    )
                }
            },
        ) {
            val fnr = "42"
            val skjema = AltinnTilgangssøknadsskjema(
                orgnr = "314",
                redirectUrl = "https://yolo.com",
                serviceCode = "1337",
                serviceEdition = 7,
            )

            val result = resolve<AltinnTilgangssoknadClient>().sendSøknad(fnr, skjema)
            assertTrue(result.status!!.isNotBlank())
            assertTrue(result.submitUrl!!.isNotBlank())
            JSONAssert.assertEquals(
                altinnSendSøknadRequest,
                capturedRequestBody.get(),
                true
            )
        }
    }

    private val continuationtoken = "hohoho"
    private val altinnHentSøknadResponse = """
        {
          "_links": {
            "next": {
              "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests?continuation=$continuationtoken"
            },
            "self": {
              "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests"
            }
          },
          "_embedded": {
            "delegationRequests": [
              {
                "Guid": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                "RequestStatus": "Created",
                "CoveredBy": "16120101181",
                "OfferedBy": "910825526",
                "RedirectUrl": "http://localhost",
                "RequestMessage": "Trenger dette for aa soeke om sykemeldinger",
                "Created": "2020-08-27T08:51:31.54",
                "LastChanged": "2020-08-27T08:51:31.54",
                "RequestResources": [
                  {
                    "ServiceCode": "4751",
                    "ServiceEditionCode": 1,
                    "Operations": [
                      "Read",
                      "Write"
                    ]
                  }
                ],
                "_links": {
                  "self": {
                    "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
                  },
                  "sendRequest": {
                    "href": "https://tt02.altinn.no/ui/DelegationRequest/send/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
                  }
                }
              }
            ]
          },
          "continuationtoken": "$continuationtoken"
        }
    """.trimIndent()
    private val altinnHentSøknadTomResponse = """
        {
          "_links": {
            "self": {
              "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests"
            }
          },
          "_embedded": {
            "delegationRequests": []
          }
        }
    """.trimIndent()

    private val altinnSendSøknadRequest = """
        {
          "CoveredBy": "42",
          "OfferedBy": "314",
          "RedirectUrl": "https://yolo.com",
          "KeepSessionAlive": true,
          "RequestResources": [
            {
              "ServiceCode": "1337",
              "ServiceEditionCode": 7
            }
          ]
        }
    """.trimIndent()

    private val altinnSendSøknadResponse = """
        {
          "Guid": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
          "RequestStatus": "Created",
          "CoveredBy": "16120101181",
          "OfferedBy": "910825526",
          "RedirectUrl": "http://localhost",
          "RequestMessage": "Trenger dette for aa soeke om sykemeldinger",
          "Created": "2020-08-27T08:51:31.54",
          "LastChanged": "2020-08-27T08:51:31.54",
          "RequestResources": [
            {
              "ServiceCode": "4751",
              "ServiceEditionCode": 1,
              "Operations": [
                "Read",
                "Write"
              ]
            }
          ],
          "_links": {
            "self": {
              "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
            },
            "sendRequest": {
              "href": "https://tt02.altinn.no/ui/DelegationRequest/send/1a9e3a32-252b-4d81-a23c-ed0d86b852c7"
            }
          }
        }
    """.trimIndent()
}