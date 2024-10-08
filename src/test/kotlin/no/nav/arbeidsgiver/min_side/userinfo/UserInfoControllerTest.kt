package no.nav.arbeidsgiver.min_side.userinfo

import no.nav.arbeidsgiver.min_side.config.GittMiljø
import no.nav.arbeidsgiver.min_side.config.SecurityConfig
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerResponse
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.filter.CharacterEncodingFilter

@MockBean(JwtDecoder::class)
@WebMvcTest(
    value = [
        UserInfoController::class,
        SecurityConfig::class,
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

    @MockBean
    lateinit var digisyfoService: DigisyfoService

    @MockBean
    lateinit var refusjonStatusService: RefusjonStatusService

    @Test
    fun `returnerer organisasjoner og rettigheter for innlogget bruker`() {
        `when`(altinnService.hentOrganisasjoner()).thenReturn(
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
                ),
            )
        )
        `when`(altinnService.hentAltinnTilganger()).thenReturn(
            AltinnTilgangerResponse(
                isError = false,
                hierarki = listOf(
                    AltinnTilgangerResponse.AltinnTilgang(
                        orgNr = "1",
                        altinn3Tilganger = setOf(),
                        altinn2Tilganger = setOf(),
                        underenheter = listOf(
                            AltinnTilgangerResponse.AltinnTilgang(
                                orgNr = "10",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("3403:1"),
                                underenheter = listOf(),
                                name = "underenhet",
                                organizationForm = "BEDR"
                            )
                        ),
                        name = "overenhet",
                        organizationForm = "AS"
                    ),
                ),
                orgNrTilTilganger = mapOf("10" to setOf("3403:1")),
                tilgangTilOrgNr = mapOf("3403:1" to setOf("10"))
            )
        )
        `when`(digisyfoService.hentVirksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoService.VirksomhetOgAntallSykmeldte(
                    Organisasjon(
                        name = "underenhet",
                        organizationNumber = "10",
                        parentOrganizationNumber = "1",
                        organizationForm = "BEDR",
                    ), 0
                ),
                DigisyfoService.VirksomhetOgAntallSykmeldte(
                    Organisasjon(
                        name = "overenhet",
                        organizationNumber = "1",
                        organizationForm = "AS",
                    ), 0
                ),
                DigisyfoService.VirksomhetOgAntallSykmeldte(
                    Organisasjon(
                        name = "underenhet",
                        organizationNumber = "20",
                        parentOrganizationNumber = "2",
                        organizationForm = "BEDR",
                    ), 1
                ),
                DigisyfoService.VirksomhetOgAntallSykmeldte(
                    Organisasjon(
                        name = "overenhet",
                        organizationNumber = "2",
                        organizationForm = "AS",
                    ), 0
                ),
            )
        )
        `when`(refusjonStatusService.statusoversikt("42")).thenReturn(
            listOf(
                RefusjonStatusService.Statusoversikt(
                    "314",
                    mapOf(
                        "ny" to 1,
                        "gammel" to 2
                    )
                ),
                RefusjonStatusService.Statusoversikt(
                    "315",
                    mapOf(
                        "ny" to 2,
                        "gammel" to 1
                    )
                ),
            )
        )

        mockMvc.get("/api/userInfo/v1") {
            with(jwtWithPid("42"))
        }.asyncDispatch().andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "altinnError": false,
                      "digisyfoError": false,
                      "digisyfoOrganisasjoner": [
                        {
                          "organisasjon": {
                            "Name": "underenhet", 
                            "ParentOrganizationNumber": "1",
                            "OrganizationNumber": "10",
                            "OrganizationForm": "BEDR"
                          },
                          "antallSykmeldte": 0
                        },
                        {
                          "organisasjon": {
                            "Name": "overenhet", 
                            "ParentOrganizationNumber": null,
                            "OrganizationNumber": "1",
                            "OrganizationForm": "AS"
                          },
                          "antallSykmeldte": 0
                        },
                        {
                          "organisasjon": {
                            "Name": "underenhet", 
                            "ParentOrganizationNumber": "2",
                            "OrganizationNumber": "20",
                            "OrganizationForm": "BEDR"
                          },
                          "antallSykmeldte": 1
                        },
                        {
                          "organisasjon": {
                            "Name": "overenhet", 
                            "ParentOrganizationNumber": null,
                            "OrganizationNumber": "2",
                            "OrganizationForm": "AS"
                          },
                          "antallSykmeldte": 0
                        }
                      ],
                      "organisasjoner": [
                        {
                          "Name": "underenhet", 
                          "ParentOrganizationNumber": "1",
                          "OrganizationNumber": "10",
                          "OrganizationForm": "BEDR"
                        },
                        {
                          "Name": "overenhet", 
                          "ParentOrganizationNumber": null,
                          "OrganizationNumber": "1",
                          "OrganizationForm": "AS"
                        }
                      ],
                      "tilganger": [
                        {
                          "tjenestekode": "3403",
                          "tjenesteversjon": "1",
                          "organisasjoner": [
                            "10"
                          ]
                        }
                      ],
                      refusjoner: [
                        {
                          "virksomhetsnummer": "314",
                          "statusoversikt": {
                            "ny": 1,
                            "gammel": 2
                          },
                          "tilgang": true
                        },
                        {
                          "virksomhetsnummer": "315",
                          "statusoversikt": {
                            "ny": 2,
                            "gammel": 1
                          },
                          "tilgang": true
                        }
                      ]
                    }    
                    """,
                    true
                )
            }
        }
    }

    @Test
    fun `returnerer http 401 for ikke innlogget bruker`() {
        mockMvc.get("/api/userInfo/v1") {
            with(anonymous())
        }.andExpect {
            status { isUnauthorized() }
        }
    }


    /**
     * Force utf 8 encoding siden AppConfig ikke benyttes i @WebMvcTest
     */
    class ForceUTF8Encoding {
        @Bean
        fun characterEncodingFilter() = CharacterEncodingFilter("UTF-8", true)
    }

}