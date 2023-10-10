package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.config.SecurityConfig
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService.VirksomhetOgAntallSykmeldte
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@MockBean(JwtDecoder::class)
@WebMvcTest(
    value = [
        DigisyfoController::class,
        SecurityConfig::class,
        AuthenticatedUserHolder::class,
    ],
    properties = [
        "server.servlet.context-path=/"
    ]
)
class DigisyfoControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var digisyfoService: DigisyfoService

    @Test
    fun `Er nærmeste leder, med sykmeldinger registrert`() {
        `when`(digisyfoService.hentVirksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                VirksomhetOgAntallSykmeldte(mkUnderenhet("10", "1"), 0),
                VirksomhetOgAntallSykmeldte(mkOverenhet("1"), 0),
                VirksomhetOgAntallSykmeldte(mkUnderenhet("20", "2"), 1),
                VirksomhetOgAntallSykmeldte(mkOverenhet("2"), 0),
            )
        )

        val jsonResponse = mockMvc
            .perform(
                get("/api/narmesteleder/virksomheter-v3")
                    .with(jwtWithPid("42"))
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        JSONAssert.assertEquals(
            """
            [
              {
                "organisasjon": {
                  "Name": "underenhet",
                  "Type": null,
                  "ParentOrganizationNumber": "1",
                  "OrganizationNumber": "10",
                  "OrganizationForm": "BEDR",
                  "Status": null
                },
                "antallSykmeldte": 0
              },
              {
                "organisasjon": {
                  "Name": "overenhet",
                  "Type": null,
                  "ParentOrganizationNumber": null,
                  "OrganizationNumber": "1",
                  "OrganizationForm": "AS",
                  "Status": null
                },
                "antallSykmeldte": 0
              },
              {
                "organisasjon": {
                  "Name": "underenhet",
                  "Type": null,
                  "ParentOrganizationNumber": "2",
                  "OrganizationNumber": "20",
                  "OrganizationForm": "BEDR",
                  "Status": null
                },
                "antallSykmeldte": 1
              },
              {
                "organisasjon": {
                  "Name": "overenhet",
                  "Type": null,
                  "ParentOrganizationNumber": null,
                  "OrganizationNumber": "2",
                  "OrganizationForm": "AS",
                  "Status": null
                },
                "antallSykmeldte": 0
              }
            ]
            """,
            jsonResponse,
            true
        )
    }


    private fun mkUnderenhet(orgnr: String, parentOrgnr: String) =
        Organisasjon(
            name = "underenhet",
            organizationNumber = orgnr,
            parentOrganizationNumber = parentOrgnr,
            organizationForm = "BEDR",
        )

    private fun mkOverenhet(orgnr: String) =
        Organisasjon(
            name = "overenhet",
            organizationNumber = orgnr,
            organizationForm = "AS",
        )
}