package no.nav.arbeidsgiver.min_side.userinfo

import no.nav.arbeidsgiver.min_side.config.GittMiljø
import no.nav.arbeidsgiver.min_side.config.SecurityConfig
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.filter.CharacterEncodingFilter

@MockitoBean(types= [JwtDecoder::class])
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

    @MockitoBean
    lateinit var altinnService: AltinnService

    @MockitoBean
    lateinit var digisyfoService: DigisyfoService

    @MockitoBean
    lateinit var refusjonStatusService: RefusjonStatusService

    @Test
    fun `returnerer organisasjoner og rettigheter for innlogget bruker`() {
        `when`(altinnService.hentAltinnTilganger()).thenReturn(
            AltinnTilganger(
                isError = false,
                hierarki = listOf(
                    AltinnTilganger.AltinnTilgang(
                        orgnr = "1",
                        altinn3Tilganger = setOf(),
                        altinn2Tilganger = setOf(),
                        underenheter = listOf(
                            AltinnTilganger.AltinnTilgang(
                                orgnr = "10",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("3403:1"),
                                underenheter = listOf(),
                                navn = "underenhet",
                                organisasjonsform = "BEDR"
                            )
                        ),
                        navn = "overenhet",
                        organisasjonsform = "AS"
                    ),
                ),
                orgNrTilTilganger = mapOf("10" to setOf("3403:1")),
                tilgangTilOrgNr = mapOf("3403:1" to setOf("10"))
            )
        )
        `when`(digisyfoService.hentVirksomheterOgSykmeldteV3("42")).thenReturn(
            listOf(
                DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                    navn = "overenhet",
                    orgnr = "1",
                    organisasjonsform = "AS",
                    antallSykmeldte = 0,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                            navn = "underenhet",
                            orgnr = "10",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 0,
                            orgnrOverenhet = "1",
                            underenheter = mutableListOf(),
                        ),
                    )
                ),
                DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                    navn = "overenhet",
                    orgnr = "2",
                    organisasjonsform = "AS",
                    antallSykmeldte = 0,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                            navn = "underenhet",
                            orgnr = "20",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 1,
                            orgnrOverenhet = "2",
                            underenheter = mutableListOf(),
                        ),
                    )
                )
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

        mockMvc.get("/api/userInfo/v3") {
            with(jwtWithPid("42"))
        }.asyncDispatch().andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "altinnError": false,
                      "digisyfoError": false,
                      "organisasjoner": [
                        {
                          "orgnr": "1",
                          "navn": "overenhet",
                          "organisasjonsform": "AS",
                          "altinn3Tilganger": [],
                          "altinn2Tilganger": [],
                          "underenheter": [
                            {
                              "orgnr": "10",
                              "navn": "underenhet",
                              "organisasjonsform": "BEDR",
                              "altinn3Tilganger": [],
                              "altinn2Tilganger": [
                                "3403:1"
                              ],
                              "underenheter": []
                            }
                          ]
                        }
                      ],
                      "tilganger": {
                        "3403:1": [
                          "10"
                        ]
                      },
                      "digisyfoOrganisasjoner": [
                        {
                          "orgnr": "1",
                          "navn": "overenhet",
                          "organisasjonsform": "AS",
                          "antallSykmeldte": 0,
                          "underenheter": [
                            {
                              "orgnr": "10",
                              "navn": "underenhet",
                              "organisasjonsform": "BEDR",
                              "antallSykmeldte": 0,
                              "underenheter": []
                            }
                          ]
                        },
                        {
                          "orgnr": "2",
                          "navn": "overenhet",
                          "organisasjonsform": "AS",
                          "antallSykmeldte": 0,
                          "underenheter": [
                            {
                              "orgnr": "20",
                              "navn": "underenhet",
                              "organisasjonsform": "BEDR",
                              "antallSykmeldte": 1,
                              "underenheter": []
                            }
                          ]
                        }
                      ],
                      "refusjoner": [
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
                    JsonCompareMode.STRICT
                )
            }
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