package no.nav.arbeidsgiver.min_side

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.infrastruktur.MockTokenIntrospector
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenIntrospector
import no.nav.arbeidsgiver.min_side.infrastruktur.configureTokenXAuth
import no.nav.arbeidsgiver.min_side.infrastruktur.mockIntrospectionResponse
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.infrastruktur.successTokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.withPid
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class AltinnTilgangerApiTest {
    @Test
    fun `returnerer altinn-tilganger for innlogget bruker`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    tjeneste = "3403:1",
                    ressurs = "nav_test_ressurs",
                    rolle = "DAGL"
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureAltinnTilgangerRoutes()
        }
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            AltinnTilgangerResponse.from(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    tjeneste = "3403:1",
                    ressurs = "nav_test_ressurs",
                    rolle = "DAGL"
                )
            ),
            response.body<AltinnTilgangerResponse>()
        )
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "roller": [
                        {
                          "kode": "DAGL",
                          "visningsnavn": "Daglig leder"
                        }
                      ],
                      "underenheter": [
                        {
                          "roller": [
                            {
                              "kode": "DAGL",
                              "visningsnavn": "Daglig leder"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent(),
            response.bodyAsText(),
            false
        )
    }
}
