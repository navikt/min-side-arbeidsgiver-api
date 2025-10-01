package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AltinnTilgangssøknadClientTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AltinnTilgangssøknadClient>(AltinnTilgangssøknadClient::class)
                provide<MaskinportenTokenService> { MaskinportenTokenServiceStub() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    /**
     * se: https://www.altinn.no/api/serviceowner/Help/Api/GET-serviceowner-delegationRequests_serviceCode_serviceEditionCode_status[0]_status[1]_continuation_coveredby_offeredby
     */
    @Test
    fun hentSøknader() = app.runTest {
        val fnr = "42"
        fakeApi.registerStub(
            HttpMethod.Get,
            "/api/serviceowner/delegationRequests",
            parametersOf(
                "ForceEIAuthentication" to listOf(""),
                "\$filter" to listOf("CoveredBy+eq+'$fnr'"),
            ),
            {
                call.response.header(HttpHeaders.ContentType, "application/json")
                call.respond(altinnHentSøknadResponse)
            }
        )
        fakeApi.registerStub(
            HttpMethod.Get,
            "/api/serviceowner/delegationRequests",
            parametersOf(
                "ForceEIAuthentication" to listOf(""),
                "\$filter" to listOf("CoveredBy+eq+'$fnr'"),
                "continuation" to listOf(continuationtoken)
            ),
            {
                call.response.header(HttpHeaders.ContentType, "application/json")
                call.respond(altinnHentSøknadTomResponse)
            }
        )

        val result = app.getDependency<AltinnTilgangssøknadClient>().hentSøknader(fnr)
        assertThat(result).isNotEmpty
    }

    /**
     * se https://www.altinn.no/api/serviceowner/Help/Api/POST-serviceowner-delegationRequests
     */
    @Test
    fun sendSøknad() = app.runTest {
        val fnr = "42"
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.com",
            serviceCode = "1337",
            serviceEdition = 7,
        )

        fakeApi.registerStub(
            HttpMethod.Post,
            "/api/serviceowner/delegationRequests",
            parametersOf("ForceEIAuthentication" to listOf("")),
        ) {
            call.response.header(HttpHeaders.ContentType, "application/json")
            call.respond(altinnSendSøknadResponse)
        }

        val result = app.getDependency<AltinnTilgangssøknadClient>().sendSøknad(fnr, skjema)
        assertThat(result.status).isNotBlank
        assertThat(result.submitUrl).isNotBlank
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
          "coveredBy": "42",
          "offeredBy": "314",
          "redirectUrl": "https://yolo.com",
          "keepSessionAlive": true,
          "requestResources": [
            {
              "serviceCode": "1337",
              "serviceEditionCode": 7
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