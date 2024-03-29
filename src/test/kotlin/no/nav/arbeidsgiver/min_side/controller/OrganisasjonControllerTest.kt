package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.config.SecurityConfig
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
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
        OrganisasjonController::class,
        SecurityConfig::class,
        AuthenticatedUserHolder::class,
    ],
    properties = [
        "server.servlet.context-path=/",
    ]
)
class OrganisasjonControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var altinnService: AltinnService

    @Test
    fun hentOrganisasjoner() {
        `when`(altinnService.hentOrganisasjoner("42")).thenReturn(
            listOf(
                Organisasjon(
                    name = "underenhet",
                    parentOrganizationNumber = "1",
                    organizationNumber = "10",
                    organizationForm = "BEDR"
                ),
                Organisasjon(
                    name = "overenhet",
                    organizationNumber = "1",
                    organizationForm = "AS"
                ),
            )
        )

        val jsonResponse = mockMvc
            .perform(
                get("/api/organisasjoner")
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
                  "Name": "underenhet",
                  "Type": null,
                  "ParentOrganizationNumber": "1",
                  "OrganizationNumber": "10",
                  "OrganizationForm": "BEDR",
                  "Status": null
              },
              {
                  "Name": "overenhet",
                  "Type": null,
                  "ParentOrganizationNumber": null,
                  "OrganizationNumber": "1",
                  "OrganizationForm": "AS",
                  "Status": null
              }
            ]
            """,
            jsonResponse,
            true
        )
    }

    @Test
    fun hentRettigheter() {
        val serviceKode = "1234"
        val serviceEdition = "1"
        `when`(altinnService.hentOrganisasjonerBasertPaRettigheter("42", serviceKode, serviceEdition)).thenReturn(
            listOf(
                Organisasjon(
                    name = "underenhet",
                    parentOrganizationNumber = "1",
                    organizationNumber = "10",
                    organizationForm = "BEDR"
                ),
                Organisasjon(
                    name = "overenhet",
                    organizationNumber = "1",
                    organizationForm = "AS"
                ),
            )
        )

        val jsonResponse = mockMvc
            .perform(
                get("/api/rettigheter-til-skjema?serviceKode=$serviceKode&serviceEdition=$serviceEdition")
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
                  "Name": "underenhet",
                  "Type": null,
                  "ParentOrganizationNumber": "1",
                  "OrganizationNumber": "10",
                  "OrganizationForm": "BEDR",
                  "Status": null
              },
              {
                  "Name": "overenhet",
                  "Type": null,
                  "ParentOrganizationNumber": null,
                  "OrganizationNumber": "1",
                  "OrganizationForm": "AS",
                  "Status": null
              }
            ]
            """,
            jsonResponse,
            true
        )
    }
}