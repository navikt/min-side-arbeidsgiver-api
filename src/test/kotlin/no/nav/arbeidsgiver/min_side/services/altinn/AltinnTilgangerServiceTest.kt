package no.nav.arbeidsgiver.min_side.services.altinn

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess


@RestClientTest(
    AltinnTilgangerService::class,
)
class AltinnTilgangerServiceTest {
    @Autowired
    lateinit var altinnServer: MockRestServiceServer

    @Autowired
    lateinit var altinnTilgangerService: AltinnTilgangerService

    @Test
    fun food(){
        altinnServer.expect(requestTo("/altinn-tilganger"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(altinnTilgangerResponse, APPLICATION_JSON)
            )

        var organisasjoner = altinnTilgangerService.hentOrganisasjoner("123")

        assertTrue(organisasjoner.size == 4)
    }
}

private val altinnTilgangerResponse = """
    {
      "isError": true,
      "hierarki": [
        {
          "orgNr": "810825472",
          "altinn3Tilganger": [],
          "altinn2Tilganger": [],
          "underenheter": [
            {
              "orgNr": "910825496",
              "altinn3Tilganger": [
                "test-fager"
              ],
              "altinn2Tilganger": [
                "4936:1"
              ],
              "underenheter": []
            }
          ]
        }
      ],
      "orgNrTilTilganger": {
        "910825496": [
          "test-fager",
          "4936:1"
        ]
      },
      "tilgangTilOrgNr": {
        "test-fager": [
          "910825496"
        ],
        "4936:1": [
          "910825496"
        ]
      }
    }
""".trimIndent()
