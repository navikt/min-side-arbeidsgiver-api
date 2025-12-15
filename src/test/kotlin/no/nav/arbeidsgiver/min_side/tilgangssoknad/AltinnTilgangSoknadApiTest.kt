package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.AltinnTilgangerMock
import no.nav.arbeidsgiver.min_side.configureTilgangssoknadRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import org.intellij.lang.annotations.Language
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class AntlinnTilgangSoknadApiTest {

    private val orgnr = "133700000"


    @Test
    fun mineSoknaderOmTilgang() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(AltinnTilgangerMock.medTilganger(orgnr = orgnr))

            hosts(AltinnTilgangssoknadClient.ingress) {
                routing {
                    install(ContentNegotiation) {
                        halJsonConfiguration()
                    }

                    get(AltinnTilgangssoknadClient.apiPath) {
                        if (call.parameters[$$"$filter"] == "CoveredBy eq '42'") {
                            if (call.parameters["continuation"] != null) {
                                call.respondText(altinnHentSoknadTomResponse, ContentType.Application.HalJson)
                            } else {
                                call.respondText(
                                    getResponse(
                                        """
                                    [
                                      {
                                        "Guid": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                                        "RequestStatus": "Created",
                                        "CoveredBy": "11111111111",
                                        "OfferedBy": "$orgnr",
                                        "RedirectUrl": "http://localhost",
                                        "RequestMessage": "Trenger dette for aa soeke om sykemeldinger",
                                        "Created": "now",
                                        "LastChanged": "whenever",
                                        "RequestResources": [
                                          {
                                            "ServiceCode": "13337",
                                            "ServiceEditionCode": 3,
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
                                            "href": "https://yolo.com"
                                          }
                                        }
                                      }
                                    ]
                                    """
                                    ),
                                    ContentType.Application.HalJson
                                )
                            }
                        }
                    }
                }
            }
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide<AltinnTilgangssoknadClient> {
                AltinnTilgangssoknadClientImpl(
                    tokenProvider = resolve(),

                    /**
                     * explicitly configure httpClient for hal+json to avoid conflicts
                     */
                    httpClient = builder.createClient {
                        halJsonHttpClientConfig()
                    },
                )
            }
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {

        val jsonResponse = client.get("ditt-nav-arbeidsgiver-api/api/altinn-tilgangssoknad") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            [
              {
                "orgnr": "$orgnr",
                "serviceCode": "13337",
                "serviceEdition": 3,
                "status": "Created",
                "createdDateTime": "now",
                "lastChangedDateTime": "whenever",
                "submitUrl": "https://yolo.com"
              }
            ]
            """, jsonResponse, true
        )
    }

    @Test
    fun sendSoknadOmTilgang() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(AltinnTilgangerMock.medTilganger(orgnr = orgnr))

            hosts(AltinnTilgangssoknadClient.ingress) {
                routing {
                    install(ContentNegotiation) {
                        halJsonConfiguration()
                    }

                    post(AltinnTilgangssoknadClient.apiPath) {
                        call.respondText(
                            //language=JSON
                            """
                             {
                                  "Guid": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                                  "RequestStatus": "Created",
                                  "CoveredBy": "11111111111",
                                  "OfferedBy": "$orgnr",
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
                                      "href": "https://yolo.com"
                                    }
                                  }
                                }
                            """.trimIndent(),
                            ContentType.Application.HalJson
                        )
                    }
                }
            }
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide<AltinnTilgangssoknadClient> {
                AltinnTilgangssoknadClientImpl(
                    tokenProvider = resolve(),
                    /**
                     * explicitly configure httpClient for hal+json to avoid conflicts
                     */
                    httpClient = builder.createClient {
                        halJsonHttpClientConfig()
                    },
                )
            }
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = orgnr,
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadService.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadService.tjenester.first().second,
        )

        val jsonResponse = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilgangssoknad") {
            contentType(ContentType.Application.Json)
            bearerAuth("faketoken")
            setBody(
                """
                    {
                        "orgnr": "${skjema.orgnr}",
                        "redirectUrl": "${skjema.redirectUrl}",
                        "serviceCode": "${skjema.serviceCode}",
                        "serviceEdition": ${skjema.serviceEdition}
                    }
                """
            )
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            it.bodyAsText()
        }

        /**
         * NB: before kotlinx.serialization jackson was configured to ignore nulls for this dto,
         * so only status and submitUrl were present.
         * but there is no such expectation in the frontend. Seems weird to have nulls ignored for just this api
         * when the frontend does not care. Adding the nulls back to the response to avoid complexity in serialization
         * config.
         * Also, why are they null? Some of the fields are actually known in the response,.
         * so it looks intentional that they are not included in the response.
         */
        JSONAssert.assertEquals(
            """
              {
                "createdDateTime": null,
                "lastChangedDateTime": null,
                "orgnr": null,
                "serviceCode": null,
                "serviceEdition": null,
                "status": "Created",
                "submitUrl": "https://yolo.com"
              }
            """, jsonResponse, true
        )
    }

    @Test
    fun sendSoknadOmTilgangSomAlleredeErSoktPaGirBadRequest() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(AltinnTilgangerMock.medTilganger(orgnr = orgnr))

            hosts(AltinnTilgangssoknadClient.ingress) {
                routing {
                    install(ContentNegotiation) {
                        halJsonConfiguration()
                    }

                    post(AltinnTilgangssoknadClient.apiPath) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            //language=JSON
                            """
                             [{"ErrorCode":"40318","ErrorMessage":"This request for access has already been registered"}]
                            """

                        )
                    }
                }
            }
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<MaskinportenTokenProvider> { successMaskinportenTokenProvider }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide<AltinnTilgangssoknadClient> {
                AltinnTilgangssoknadClientImpl(
                    tokenProvider = resolve(),

                    /**
                     * explicitly configure httpClient for hal+json to avoid conflicts
                     */
                    httpClient = builder.createClient {
                        halJsonHttpClientConfig()
                    },
                )
            }
            provide(AltinnTilgangSoknadService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureTilgangssoknadRoutes()
        },
    ) {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadService.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadService.tjenester.first().second,
        )


        client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilgangssoknad") {
            contentType(ContentType.Application.Json)
            bearerAuth("faketoken")
            setBody(
            """
            {
                "orgnr": "${skjema.orgnr}",
                "redirectUrl": "${skjema.redirectUrl}",
                "serviceCode": "${skjema.serviceCode}",
                "serviceEdition": ${skjema.serviceEdition}
            }
            """
            )
        }.let {
            assertEquals(HttpStatusCode.BadRequest, it.status)
        }

    }
}

private val altinnHentSoknadTomResponse = """
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

private fun getResponse(@Language("JSON") delegationRequests: String) = """
        {
          "_links": {
            "next": {
              "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests?continuation=fancytoken"
            },
            "self": {
              "href": "https://tt02.altinn.no/api/serviceowner/delegationrequests"
            }
          },
          "_embedded": {
            "delegationRequests": $delegationRequests
          },
          "continuationtoken": "fancytoken"
        }
    """.trimIndent()