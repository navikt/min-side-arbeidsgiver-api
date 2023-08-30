package no.nav.arbeidsgiver.min_side.userinfo

import no.nav.arbeidsgiver.min_side.SecurityConfiguration
import no.nav.arbeidsgiver.min_side.config.GittMiljø
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@MockBean(JwtDecoder::class)
@WebMvcTest(
    value = [
        UserInfoController::class,
        SecurityConfiguration::class,
        AuthenticatedUserHolder::class,
        GittMiljø::class,
    ],
    properties = [
        "server.servlet.context-path=/"
    ]
)
class UserInfoControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var altinnService: AltinnService

    @Test
    fun `returnerer organisasjoner og rettigheter for innlogget bruker`() {
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
                    organizationForm = "AS",
                    type = "Enterprise"

                ),
            )
        )
        `when`(altinnService.hentOrganisasjonerBasertPaRettigheter(anyString(), anyString(), anyString())).then {
            if (it.arguments[1] == "3403" && it.arguments[2] == "1") {
                listOf(
                    Organisasjon(
                        name = "underenhet",
                        parentOrganizationNumber = "1",
                        organizationNumber = "10",
                        organizationForm = "BEDR"
                    ),
                )
            } else if (it.arguments[1] == "5516") {
                throw RuntimeException("Kan ikke hente organisasjoner")
            } else {
                emptyList()
            }
        }

        mockMvc
            .perform(
                get("/api/userInfo/v1")
                    .with(jwtWithPid("42"))
            )
            .andExpect(request().asyncStarted())
            .andExpect(request().asyncResult(notNullValue()))
            .andReturn().let { mvcresult ->
                mockMvc.perform(asyncDispatch(mvcresult))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString.let {
                        JSONAssert.assertEquals(
                            """
                            {
                              "altinnError": true,
                              "organisasjoner": [
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
                                      "Type": "Enterprise",
                                      "ParentOrganizationNumber": null,
                                      "OrganizationNumber": "1",
                                      "OrganizationForm": "AS",
                                      "Status": null
                                  }
                                ],
                                "tilganger": [
                                    {
                                        "id": "sykefravarstatistikk",
                                        "tjenestekode": "3403",
                                        "tjenesteversjon": "1",
                                        "organisasjoner": [ "10" ]
                                    }
                                ]
                            }
                            """,
                            it,
                            true
                        )
                    }
            }
    }

    @Test
    fun `returnerer http 401 for ikke innlogget bruker`() {
        mockMvc
            .perform(
                get("/api/userInfo/v1")
                    .with(anonymous())
            )
            .andExpect(status().isUnauthorized)
    }
}