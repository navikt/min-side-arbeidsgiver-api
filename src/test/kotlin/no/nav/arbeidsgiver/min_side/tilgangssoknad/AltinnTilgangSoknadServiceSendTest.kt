package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert

class AntlinnTilgangSoknadServiceHentTest {
    companion object {

        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AltinnTilgangssøknadClient> { Mockito.mock<AltinnTilgangssøknadClient>() }
                provide<AltinnTilgangSoknadService>(AltinnTilgangSoknadService::class)
                provide<AltinnService> { Mockito.mock<AltinnService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun mineSøknaderOmTilgang() = app.runTest {
        val altinnTilgangssøknad = AltinnTilgangssøknad(
            orgnr = "314",
            serviceCode = "13337",
            serviceEdition = 3,
            status = "Created",
            createdDateTime = "now",
            lastChangedDateTime = "whenever",
            submitUrl = "https://yolo.com",
        )
        val altinnTilgangssøknadClient = app.getDependency<AltinnTilgangssøknadClient>()
        `when`(altinnTilgangssøknadClient.hentSøknader("42")).thenReturn(listOf(altinnTilgangssøknad))

        val jsonResponse = client.get("ditt-nav-arbeidsgiver-api/api/altinn-tilgangssoknad") {
            bearerAuth(fakeToken("42"))
            accept(ContentType.Application.Json)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            [
              {
                "orgnr": "314",
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
}


class AltinnTilgangSoknadServiceSendTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AltinnTilgangssøknadClient>(AltinnTilgangssøknadClient::class)
                provide<AltinnTilgangSoknadService>(AltinnTilgangSoknadService::class)
                provide<MaskinportenTokenService>(MaskinportenTokenServiceStub::class)
                provide<AltinnService> { Mockito.mock<AltinnService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun sendSøknadOmTilgang() = app.runTest {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadService.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadService.tjenester.first().second,
        )

        val status = "Created"
        val submitUrl = "https://yolo.com"

        val altinnService = app.getDependency<AltinnService>()
//        val altinnTilgangssøknadClient = app.getDependency<AltinnTilgangssøknadClient>()
        val token = fakeToken("42")

        fakeApi.registerStub(HttpMethod.Post, "/api/serviceowner/delegationRequests") {
            call.response.headers.append(HttpHeaders.ContentType, ContentType.Application.HalJson.toString())
            call.respond(
                """
                 {
                      "Guid": "1a9e3a32-252b-4d81-a23c-ed0d86b852c7",
                      "RequestStatus": "$status",
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
                          "href": "$submitUrl"
                        }
                      }
                    }
                """.trimIndent()
            )
        }

        `when`(altinnService.harOrganisasjon(skjema.orgnr, token)).thenReturn(true)

        val jsonResponse = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilgangssoknad") {
            bearerAuth(token)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
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
            assert(it.status == HttpStatusCode.OK)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
              {
                "status": "$status",
                "submitUrl": "$submitUrl"
              }
            """, jsonResponse, true
        )
    }

    @Test
    fun sendSøknadOmTilgangSomAlleredeErSøktPåGirBadRequest() = app.runTest {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadService.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadService.tjenester.first().second,
        )

        fakeApi.registerStub(HttpMethod.Post, "/api/serviceowner/delegationRequests") {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond("""[{"ErrorCode":"40318","ErrorMessage":"This request for access has already been registered"}]""")
        }

        val altinnService = app.getDependency<AltinnService>()
        val token = fakeToken("42")

        `when`(altinnService.harOrganisasjon("314", token)).thenReturn(true)

        client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilgangssoknad") {
            bearerAuth(token)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
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
            assert(it.status == HttpStatusCode.BadRequest)
        }

    }
}