package no.nav.arbeidsgiver.min_side.clients.altinn

import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknadsskjema
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnConfig
import no.nav.arbeidsgiver.min_side.services.tokenExchange.ClientAssertionTokenFactory
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@MockBean(MultiIssuerConfiguration::class, ClientAssertionTokenFactory::class)
@RestClientTest(AltinnTilgangssøknadClient::class)
@EnableConfigurationProperties(AltinnConfig::class)
class AltinnTilgangssøknadClientTest {
    @Autowired
    lateinit var server: MockRestServiceServer

    @Autowired
    lateinit var client: AltinnTilgangssøknadClient

    /**
     * se: https://www.altinn.no/api/serviceowner/Help/Api/GET-serviceowner-delegationRequests_serviceCode_serviceEditionCode_status[0]_status[1]_continuation_coveredby_offeredby
     */
    @Test
    fun hentSøknader() {
        val fnr = "42"
        server.expect(requestToUriTemplate("http://localhost:8081/altinn/ekstern/altinn/api/serviceowner/delegationRequests?ForceEIAuthentication&\$filter={filter}", "CoveredBy eq '$fnr'"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(altinnHentSøknadResponse, MediaType.APPLICATION_JSON))
        server.expect(requestToUriTemplate("http://localhost:8081/altinn/ekstern/altinn/api/serviceowner/delegationRequests?ForceEIAuthentication&\$filter={filter}&continuation={continuation}", "CoveredBy eq '$fnr'", continuationtoken))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(altinnHentSøknadTomResponse, MediaType.APPLICATION_JSON))

        val result = client.hentSøknader(fnr)
        assertThat(result).isNotEmpty
    }

    /**
     * se https://www.altinn.no/api/serviceowner/Help/Api/POST-serviceowner-delegationRequests
     */
    @Test
    fun sendSøknad() {
        val fnr = "42"
        val skjema = AltinnTilgangssøknadsskjema()
        skjema.orgnr = "314"
        skjema.redirectUrl = "https://yolo.com"
        skjema.serviceCode = "1337"
        skjema.serviceEdition = 7

        server.expect(requestToUriTemplate("http://localhost:8081/altinn/ekstern/altinn/api/serviceowner/delegationRequests?ForceEIAuthentication"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(altinnSendSøknadRequest))
            .andRespond(withSuccess(altinnSendSøknadResponse, MediaType.APPLICATION_JSON))

        val result = client.sendSøknad(fnr, skjema)
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