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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.filter.CharacterEncodingFilter

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
@Import(UserInfoControllerTest.ForceUTF8Encoding::class)
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
                                      "id": "ekspertbistand",
                                      "tjenestekode": "5384",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "inntektsmelding",
                                      "tjenestekode": "4936",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "utsendtArbeidstakerEØS",
                                      "tjenestekode": "4826",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "arbeidstrening",
                                      "tjenestekode": "5332",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "arbeidsforhold",
                                      "tjenestekode": "5441",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "midlertidigLønnstilskudd",
                                      "tjenestekode": "5516",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "varigLønnstilskudd",
                                      "tjenestekode": "5516",
                                      "tjenesteversjon": "2",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "sommerjobb",
                                      "tjenestekode": "5516",
                                      "tjenesteversjon": "3",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "mentortilskudd",
                                      "tjenestekode": "5516",
                                      "tjenesteversjon": "4",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "inkluderingstilskudd",
                                      "tjenestekode": "5516",
                                      "tjenesteversjon": "5",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "sykefravarstatistikk",
                                      "tjenestekode": "3403",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": [
                                        "10"
                                      ]
                                    },
                                    {
                                      "id": "forebyggefravar",
                                      "tjenestekode": "5934",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "rekruttering",
                                      "tjenestekode": "5078",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "tilskuddsbrev",
                                      "tjenestekode": "5278",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
                                    },
                                    {
                                      "id": "yrkesskade",
                                      "tjenestekode": "5902",
                                      "tjenesteversjon": "1",
                                      "organisasjoner": []
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


    /**
     * Force utf 8 encoding siden AppConfig ikke benyttes i @WebMvcTest
     */
    class ForceUTF8Encoding {
        @Bean
        fun characterEncodingFilter() = CharacterEncodingFilter("UTF-8", true)
    }
}