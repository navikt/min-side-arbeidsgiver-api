package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post


@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.enabled=false",
    ]
)
@AutoConfigureMockMvc
class EregControllerTest {

    @MockitoBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @Autowired
    lateinit var mockMvc: MockMvc

    lateinit var server: MockRestServiceServer

    @Autowired
    lateinit var eregService: EregService

    @BeforeEach
    fun setUp() {
        server = MockRestServiceServer.bindTo(eregService.restTemplate).build()
    }


    @Test
    fun `overenhet som mangler en del felter`() {
        val orgnr = "313199770"
        server.expect(requestTo("https://localhost/v2/organisasjon/$orgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(overenhetUtenEnDelFelter, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$orgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(orgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "313199770",
                      "navn": "ALLSIDIG UTMERKET TIGER AS",
                      "organisasjonsform": {
                        "kode": "AS",
                        "beskrivelse": "Aksjeselskap"
                      },
                      "naeringskoder": null,
                      "postadresse": null,
                      "forretningsadresse": null,
                      "hjemmeside": null,
                      "overordnetEnhet": null,
                      "antallAnsatte": 2,
                      "beliggenhetsadresse": null
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }


    @Test
    fun `henter underenhet fra ereg`() {
        val virksomhetsnummer = "910825526"
        server.expect(requestTo("https://localhost/v2/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(underenhetRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/underenhet") {
            content = """{"orgnr": "$virksomhetsnummer"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(virksomhetsnummer))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "910825526",
                      "navn": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA P",
                      "organisasjonsform": {
                        "kode": "BEDR",
                        "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning"
                      },
                      "naeringskoder": null,
                      "postadresse": {
                        "adresse": "PERSONALKONTORET, PHILIP LUNDQUIST, POSTBOKS 144",                    
                        "kommunenummer": "1120",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "4358",
                        "poststed": "KLEPPE"
                      },
                      "forretningsadresse": {
                        "adresse": "AVDELING HORTEN, VED PHILIP LUNDQUI ST, APOTEKERGATA 16",
                        "kommunenummer": "3801",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "3187",
                        "poststed": "HORTEN"
                      },
                      "hjemmeside": null,
                      "overordnetEnhet": "810825472",
                      "antallAnsatte": 10,
                      "beliggenhetsadresse": {
                        "adresse": "AVDELING HORTEN, VED PHILIP LUNDQUI ST, APOTEKERGATA 16",
                        "kommunenummer": "3801",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "3187",
                        "poststed": "HORTEN"
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }


    @Test
    fun `henter underenhet med orgledd fra ereg`() {
        val virksomhetsnummer = "912998827"
        server.expect(requestTo("https://localhost/v2/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(underenhetMedOrgleddRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/underenhet") {
            content = """{"orgnr": "$virksomhetsnummer"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(virksomhetsnummer))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "912998827",
                      "navn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD ØKERNVEIEN",
                      "organisasjonsform": {
                        "kode": "BEDR",
                        "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning"
                      },
                      "naeringskoder": [
                        "84.120"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 5    St. Olavs Plass",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0130",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      },
                      "hjemmeside": null,
                      "overordnetEnhet": "889640782",
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `underenhet er null fra ereg`() {
        val virksomhetsnummer = "12345678"
        server.expect(requestTo("https://localhost/v2/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .body(underenhetIkkeFunnetRespons)
                    .contentType(APPLICATION_JSON)
            )

        mockMvc.post("/api/ereg/underenhet") {
            content = """{"orgnr": "$virksomhetsnummer"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(virksomhetsnummer))
        }.andExpect {
            status { isOk() }
            content {
                bytes(ByteArray(0))
            }
        }
    }

    @Test
    fun `henter overenhet fra ereg`() {
        val orgnr = "810825472"
        server.expect(requestTo("https://localhost/v2/organisasjon/$orgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(overenhetRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$orgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(orgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                   {
                      "organisasjonsnummer": "810825472",
                      "navn": "MALMEFJORD OG RIDABU REGNSKAP",
                      "organisasjonsform": {
                        "kode": "AS",
                        "beskrivelse": "Aksjeselskap"
                      },
                      "naeringskoder": null,
                      "postadresse": {
                        "adresse": "POSTBOKS 4120",
                        "kommunenummer": "3403",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "2307",
                        "poststed": "HAMAR"
                      },
                      "forretningsadresse": {
                        "adresse": "RÅDHUSET",
                        "kommunenummer": "1579",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "6440",
                        "poststed": "ELNESVÅGEN"
                      },
                      "hjemmeside": null,
                      "overordnetEnhet": null,
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "RÅDHUSET",
                        "kommunenummer": "1579",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "6440",
                        "poststed": "ELNESVÅGEN"
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `henter orgledd fra ereg`() {
        val orgnr = "889640782"
        server.expect(requestTo("https://localhost/v2/organisasjon/$orgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(orgleddRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$orgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(orgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "889640782",
                      "navn": "ARBEIDS- OG VELFERDSETATEN",
                      "organisasjonsform": {
                        "kode": "ORGL",
                        "beskrivelse": "Organisasjonsledd"
                      },
                      "naeringskoder": [
                        "84.120"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 5 St Olavs Plass",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0130",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      },
                      "hjemmeside": "www.nav.no",
                      "overordnetEnhet": "983887457",
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }


    @Test
    fun `henter jurudisk enhet for orgledd 889640782`() {
        val orgnr = "983887457"
        server.expect(requestTo("https://localhost/v2/organisasjon/$orgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(juridiskEnhetForOrgleddRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$orgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(orgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "983887457",
                      "navn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
                      "organisasjonsform": {
                        "kode": "STAT",
                        "beskrivelse": "Staten"
                      },
                      "naeringskoder": [
                          "84.110"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 8019 Dep",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0030",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Akersgata 64",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0180",
                        "poststed": null
                      },
                      "hjemmeside": "regjeringen.no/asd",
                      "overordnetEnhet": null,
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Akersgata 64",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0180",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `overenhet er null fra ereg`() {
        val orgnr = "314"
        server.expect(requestTo("https://localhost/v2/organisasjon/$orgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .body(overenhetIkkeFunnetRespons)
                    .contentType(APPLICATION_JSON)
            )

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$orgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(orgnr))
        }.andExpect {
            status { isOk() }
            content {
                bytes(ByteArray(0))
            }
        }
    }


    @Test
    fun `skal klare å gå fra BEDR til STAT via hierarki for NAV IKT`() {
        val naviktOrgnr = "990983666"
        val naviktOrglOrgnr = "990983291"
        val velferdsEtatOrgnr = "889640782"
        val sosDepOrgnr = "983887457"

        server.expect(requestTo("https://localhost/v2/organisasjon/$naviktOrgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(naviktRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$naviktOrgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(naviktOrgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "990983666",
                      "navn": "NAV IKT",
                      "organisasjonsform": {
                        "kode": "BEDR",
                        "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning"
                      },
                      "naeringskoder": [
                        "84.300"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 5 St Olavs plass",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0130",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Sannergata 2",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0557",
                        "poststed": null
                      },
                      "hjemmeside": null,
                      "overordnetEnhet": "$naviktOrglOrgnr",
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Sannergata 2",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0557",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }

        server.reset()
        server.expect(requestTo("https://localhost/v2/organisasjon/$naviktOrglOrgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(orgleddRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$naviktOrglOrgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(naviktOrgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "889640782",
                      "navn": "ARBEIDS- OG VELFERDSETATEN",
                      "organisasjonsform": {
                        "kode": "ORGL",
                        "beskrivelse": "Organisasjonsledd"
                      },
                      "naeringskoder": [
                        "84.120"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 5 St Olavs Plass",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0130",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      },
                      "hjemmeside": "www.nav.no",
                      "overordnetEnhet": "983887457",
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }

        server.reset()
        server.expect(requestTo("https://localhost/v2/organisasjon/$velferdsEtatOrgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(velferdsEtatRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$velferdsEtatOrgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(naviktOrgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "889640782",
                      "navn": "ARBEIDS- OG VELFERDSETATEN",
                      "organisasjonsform": {
                        "kode": "ORGL",
                        "beskrivelse": "Organisasjonsledd"
                      },
                      "naeringskoder": [
                        "84.120"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 5 St Olavs Plass",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0130",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      },
                      "hjemmeside": "www.nav.no",
                      "overordnetEnhet": "983887457",
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Økernveien 94",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0579",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }

        server.reset()
        server.expect(requestTo("https://localhost/v2/organisasjon/$sosDepOrgnr?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(sosDepRespons, APPLICATION_JSON))

        mockMvc.post("/api/ereg/overenhet") {
            content = """{"orgnr": "$sosDepOrgnr"}"""
            contentType = APPLICATION_JSON
            with(jwtWithPid(naviktOrgnr))
        }.andExpect {
            status { isOk() }
            content {
                json(
                    """
                    {
                      "organisasjonsnummer": "983887457",
                      "navn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
                      "organisasjonsform": {
                        "kode": "STAT",
                        "beskrivelse": "Staten"
                      },
                      "naeringskoder": [
                        "84.110"
                      ],
                      "postadresse": {
                        "adresse": "Postboks 8019 Dep",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0030",
                        "poststed": null
                      },
                      "forretningsadresse": {
                        "adresse": "Akersgata 64",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0180",
                        "poststed": null
                      },
                      "hjemmeside": "regjeringen.no/asd",
                      "overordnetEnhet": null,
                      "antallAnsatte": null,
                      "beliggenhetsadresse": {
                        "adresse": "Akersgata 64",
                        "kommunenummer": "0301",
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0180",
                        "poststed": null
                      }
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT
                )
            }
        }
    }
}

//Responsene er hentet fra https://ereg-services.dev.intern.nav.no/swagger-ui/index.html#/organisasjon.v1/hentOrganisasjonUsingGET

//language=JSON
private const val underenhetRespons = """
{
  "organisasjonsnummer": "910825526",
  "type": "Virksomhet",
  "navn": {
    "sammensattnavn": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA P",
    "navnelinje1": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA",
    "navnelinje2": "P",
    "bruksperiode": {
      "fom": "2020-09-03T09:00:32.733"
    },
    "gyldighetsperiode": {
      "fom": "2020-09-03"
    }
  },
  "organisasjonDetaljer": {
    "ansatte": [
        {
            "antall": 10,
            "bruksperiode": {
              "fom": "2020-09-03T09:00:32.693"
            },
            "gyldighetsperiode": {
              "fom": "2020-09-03"
            } 
        }
    ],
    "registreringsdato": "2019-07-11T00:00:00",
    "enhetstyper": [
      {
        "enhetstype": "BEDR",
        "bruksperiode": {
          "fom": "2019-07-11T11:59:24.72"
        },
        "gyldighetsperiode": {
          "fom": "2019-07-11"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA P",
        "navnelinje1": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA",
        "navnelinje2": "P",
        "bruksperiode": {
          "fom": "2020-09-03T09:00:32.733"
        },
        "gyldighetsperiode": {
          "fom": "2020-09-03"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "AVDELING HORTEN, VED PHILIP LUNDQUI",
        "adresselinje2": "ST, APOTEKERGATA 16",
        "postnummer": "3187",
        "poststed": "HORTEN",
        "landkode": "NO",
        "kommunenummer": "3801",
        "bruksperiode": {
          "fom": "2020-09-03T09:00:32.693"
        },
        "gyldighetsperiode": {
          "fom": "2020-09-03"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "PERSONALKONTORET, PHILIP LUNDQUIST,",
        "adresselinje2": "POSTBOKS 144",
        "postnummer": "4358",
        "poststed": "KLEPPE",
        "landkode": "NO",
        "kommunenummer": "1120",
        "bruksperiode": {
          "fom": "2020-09-03T09:00:32.685"
        },
        "gyldighetsperiode": {
          "fom": "2020-09-03"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": false,
      "bruksperiode": {
        "fom": "1900-01-01T00:00:00"
      },
      "gyldighetsperiode": {
        "fom": "1900-01-01"
      }
    },
    "sistEndret": "2020-09-03"
  },
  "virksomhetDetaljer": {
    "enhetstype": "BEDR"
  },
  "inngaarIJuridiskEnheter": [
    {
      "organisasjonsnummer": "810825472",
      "navn": {
        "sammensattnavn": "MALMEFJORD OG RIDABU REGNSKAP",
        "navnelinje1": "MALMEFJORD OG RIDABU REGNSKAP",
        "bruksperiode": {
          "fom": "2020-05-14T16:03:21.12"
        },
        "gyldighetsperiode": {
          "fom": "2020-05-14"
        }
      },
      "bruksperiode": {
        "fom": "2020-09-03T09:00:32.718"
      },
      "gyldighetsperiode": {
        "fom": "2020-09-03"
      }
    }
  ]
}
"""

//language=JSON
private const val overenhetRespons = """
{
  "organisasjonsnummer": "810825472",
  "type": "JuridiskEnhet",
  "navn": {
    "sammensattnavn": "MALMEFJORD OG RIDABU REGNSKAP",
    "navnelinje1": "MALMEFJORD OG RIDABU REGNSKAP",
    "bruksperiode": {
      "fom": "2020-05-14T16:03:21.12"
    },
    "gyldighetsperiode": {
      "fom": "2020-05-14"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2019-07-11T00:00:00",
    "enhetstyper": [
      {
        "enhetstype": "AS",
        "bruksperiode": {
          "fom": "2019-07-11T11:59:24.704"
        },
        "gyldighetsperiode": {
          "fom": "2019-07-11"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "MALMEFJORD OG RIDABU REGNSKAP",
        "navnelinje1": "MALMEFJORD OG RIDABU REGNSKAP",
        "bruksperiode": {
          "fom": "2020-05-14T16:03:21.12"
        },
        "gyldighetsperiode": {
          "fom": "2020-05-14"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "RÅDHUSET",
        "postnummer": "6440",
        "poststed": "ELNESVÅGEN",
        "landkode": "NO",
        "kommunenummer": "1579",
        "bruksperiode": {
          "fom": "2020-05-14T16:03:21.144"
        },
        "gyldighetsperiode": {
          "fom": "2020-05-14"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "POSTBOKS 4120",
        "postnummer": "2307",
        "poststed": "HAMAR",
        "landkode": "NO",
        "kommunenummer": "3403",
        "bruksperiode": {
          "fom": "2020-05-14T16:03:21.126"
        },
        "gyldighetsperiode": {
          "fom": "2020-05-14"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": false,
      "bruksperiode": {
        "fom": "1900-01-01T00:00:00"
      },
      "gyldighetsperiode": {
        "fom": "1900-01-01"
      }
    },
    "sistEndret": "2020-05-14"
  },
  "juridiskEnhetDetaljer": {
    "enhetstype": "AS"
  },
  "driverVirksomheter": [
    {
      "organisasjonsnummer": "910825526",
      "navn": {
        "sammensattnavn": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA P",
        "navnelinje1": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA",
        "navnelinje2": "P",
        "bruksperiode": {
          "fom": "2020-09-03T09:00:32.733"
        },
        "gyldighetsperiode": {
          "fom": "2020-09-03"
        }
      },
      "bruksperiode": {
        "fom": "2020-09-03T09:00:32.718"
      },
      "gyldighetsperiode": {
        "fom": "2020-09-03"
      }
    },
    {
      "organisasjonsnummer": "910825518",
      "navn": {
        "sammensattnavn": "MAURA OG KOLBU REGNSKAP",
        "navnelinje1": "MAURA OG KOLBU REGNSKAP",
        "bruksperiode": {
          "fom": "2020-05-14T16:03:20.956"
        },
        "gyldighetsperiode": {
          "fom": "2020-05-14"
        }
      },
      "bruksperiode": {
        "fom": "2020-05-14T16:03:20.962"
      },
      "gyldighetsperiode": {
        "fom": "2020-05-14"
      }
    }
  ]
}
"""

//language=JSON
private const val underenhetMedOrgleddRespons = """
{
  "organisasjonsnummer": "912998827",
  "type": "Virksomhet",
  "navn": {
    "sammensattnavn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD ØKERNVEIEN",
    "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
    "navnelinje3": "AVD ØKERNVEIEN",
    "bruksperiode": {
      "fom": "2015-02-23T08:04:53.2"
    },
    "gyldighetsperiode": {
      "fom": "2013-12-23"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2013-12-23T00:00:00",
    "enhetstyper": [
      {
        "enhetstype": "BEDR",
        "bruksperiode": {
          "fom": "2014-05-21T15:05:45.667"
        },
        "gyldighetsperiode": {
          "fom": "2013-12-23"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD ØKERNVEIEN",
        "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
        "navnelinje3": "AVD ØKERNVEIEN",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2013-12-23"
        }
      }
    ],
    "naeringer": [
      {
        "naeringskode": "84.120",
        "hjelpeenhet": false,
        "bruksperiode": {
          "fom": "2014-05-22T00:48:03.133"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "Økernveien 94",
        "postnummer": "0579",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2013-12-23"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "Postboks 5    St. Olavs Plass",
        "postnummer": "0130",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2013-12-23"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": true,
      "bruksperiode": {
        "fom": "2014-12-08T10:42:54.425"
      },
      "gyldighetsperiode": {
        "fom": "2014-12-08"
      }
    },
    "sistEndret": "2014-03-18"
  },
  "virksomhetDetaljer": {
    "enhetstype": "BEDR",
    "oppstartsdato": "2013-12-01"
  },
  "bestaarAvOrganisasjonsledd": [
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "889640782",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
          "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "2006-03-23"
          }
        },
        "inngaarIJuridiskEnheter": [
          {
            "organisasjonsnummer": "983887457",
            "navn": {
              "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
              "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
              "bruksperiode": {
                "fom": "2015-02-23T08:04:53.2"
              },
              "gyldighetsperiode": {
                "fom": "2014-02-14"
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T15:42:14.826"
            },
            "gyldighetsperiode": {
              "fom": "2006-03-23"
            }
          }
        ]
      },
      "bruksperiode": {
        "fom": "2014-05-23T16:08:14.385"
      },
      "gyldighetsperiode": {
        "fom": "2013-12-23"
      }
    }
  ]
}
"""

//language=JSON
private const val orgleddRespons = """
{
  "organisasjonsnummer": "889640782",
  "type": "Organisasjonsledd",
  "navn": {
    "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
    "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
    "bruksperiode": {
      "fom": "2015-02-23T08:04:53.2"
    },
    "gyldighetsperiode": {
      "fom": "2006-03-23"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2006-03-23T00:00:00",
    "stiftelsesdato": "2005-12-31",
    "enhetstyper": [
      {
        "enhetstype": "ORGL",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2006-03-23"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
        "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2006-03-23"
        }
      }
    ],
    "naeringer": [
      {
        "naeringskode": "84.120",
        "hjelpeenhet": false,
        "bruksperiode": {
          "fom": "2014-05-22T00:42:43.812"
        },
        "gyldighetsperiode": {
          "fom": "2005-12-31"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "Økernveien 94",
        "postnummer": "0579",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2014-07-13T04:02:31.517"
        },
        "gyldighetsperiode": {
          "fom": "2014-07-12"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "Postboks 5 St Olavs Plass",
        "postnummer": "0130",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2010-11-03"
        }
      }
    ],
    "internettadresser": [
      {
        "adresse": "www.nav.no",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:37.997"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "telefonnummer": [
      {
        "nummer": "21 07 00 00",
        "telefontype": "TFON",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "telefaksnummer": [
      {
        "nummer": "21 07 00 01",
        "telefontype": "TFAX",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "formaal": [
      {
        "formaal": "Arbeids- og velferdsetaten har ansvaret for gjennomføringen av\narbeidsmarkeds- trygde- og pensjonspolitikken",
        "bruksperiode": {
          "fom": "2014-05-21T23:46:16.225"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "registrertMVA": [
      {
        "registrertIMVA": true,
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": true,
      "bruksperiode": {
        "fom": "2014-12-29T09:43:08.386"
      },
      "gyldighetsperiode": {
        "fom": "2014-12-29"
      }
    },
    "maalform": "NB",
    "sistEndret": "2015-10-07"
  },
  "organisasjonsleddDetaljer": {
    "enhetstype": "ORGL",
    "sektorkode": "6100"
  },
  "driverVirksomheter": [
    {
      "organisasjonsnummer": "991003525",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN IKT DRIFT STEINKJER",
        "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
        "navnelinje3": "IKT DRIFT STEINKJER",
        "bruksperiode": {
          "fom": "2018-01-11T04:00:43.413"
        },
        "gyldighetsperiode": {
          "fom": "2018-01-10"
        }
      },
      "bruksperiode": {
        "fom": "2018-01-11T04:00:53.145"
      },
      "gyldighetsperiode": {
        "fom": "2018-01-10"
      }
    },
    {
      "organisasjonsnummer": "912998827",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD ØKERNVEIEN",
        "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
        "navnelinje3": "AVD ØKERNVEIEN",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2013-12-23"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T16:08:14.385"
      },
      "gyldighetsperiode": {
        "fom": "2013-12-23"
      }
    },
    {
      "organisasjonsnummer": "986001344",
      "navn": {
        "sammensattnavn": "NAV ABETAL",
        "navnelinje1": "NAV ABETAL",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2010-04-09"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T19:26:04.911"
      },
      "gyldighetsperiode": {
        "fom": "2010-04-12"
      }
    },
    {
      "organisasjonsnummer": "995298775",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD SANNERGATA",
        "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
        "navnelinje3": "AVD SANNERGATA",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2010-03-16"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T21:01:47.564"
      },
      "gyldighetsperiode": {
        "fom": "2010-03-16"
      }
    }
  ],
  "inngaarIJuridiskEnheter": [
    {
      "organisasjonsnummer": "983887457",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-14"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T15:42:14.826"
      },
      "gyldighetsperiode": {
        "fom": "2006-03-23"
      }
    }
  ],
  "organisasjonsleddUnder": [
    {
      "organisasjonsledd": {
        "type": "Organisasjonsledd",
        "organisasjonsleddUnder": [
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "995277670",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV ØKONOMILINJEN",
                "navnelinje1": "NAV ØKONOMILINJEN",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2013-12-24"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T21:01:30.126"
            },
            "gyldighetsperiode": {
              "fom": "2010-03-11"
            }
          },
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "991012206",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV YTELSESLINJEN",
                "navnelinje1": "NAV YTELSESLINJEN",
                "bruksperiode": {
                  "fom": "2014-05-21T16:53:56.633"
                },
                "gyldighetsperiode": {
                  "fom": "2014-01-07"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:58.91"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-12"
            }
          },
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "991012133",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV ARBEIDS- OG TJENESTELINJEN",
                "navnelinje1": "NAV ARBEIDS- OG TJENESTELINJEN",
                "bruksperiode": {
                  "fom": "2016-02-02T04:02:14.967"
                },
                "gyldighetsperiode": {
                  "fom": "2016-02-01"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:58.879"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-12"
            }
          },
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "990983291",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV IKT",
                "navnelinje1": "NAV IKT",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2010-04-09"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:42.452"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-05"
            }
          }
        ]
      },
      "bruksperiode": {},
      "gyldighetsperiode": {}
    }
  ]
}
"""

//language=JSON
private const val juridiskEnhetForOrgleddRespons = """
 {
  "organisasjonsnummer": "983887457",
  "type": "JuridiskEnhet",
  "navn": {
    "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
    "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
    "bruksperiode": {
      "fom": "2015-02-23T08:04:53.2"
    },
    "gyldighetsperiode": {
      "fom": "2014-02-14"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2001-11-08T00:00:00",
    "enhetstyper": [
      {
        "enhetstype": "STAT",
        "bruksperiode": {
          "fom": "2014-05-21T21:07:05.975"
        },
        "gyldighetsperiode": {
          "fom": "2001-11-08"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-14"
        }
      }
    ],
    "naeringer": [
      {
        "naeringskode": "84.110",
        "hjelpeenhet": false,
        "bruksperiode": {
          "fom": "2014-05-22T01:07:26.409"
        },
        "gyldighetsperiode": {
          "fom": "2001-11-08"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "Akersgata 64",
        "postnummer": "0180",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2012-02-07"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "Postboks 8019 Dep",
        "postnummer": "0030",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2004-10-15"
        }
      }
    ],
    "epostadresser": [
      {
        "adresse": "postmottak@asd.dep.no",
        "bruksperiode": {
          "fom": "2016-08-02T04:03:15.626"
        },
        "gyldighetsperiode": {
          "fom": "2016-08-01"
        }
      }
    ],
    "internettadresser": [
      {
        "adresse": "regjeringen.no/asd",
        "bruksperiode": {
          "fom": "2016-08-02T04:03:15.641"
        },
        "gyldighetsperiode": {
          "fom": "2016-08-01"
        }
      }
    ],
    "telefonnummer": [
      {
        "nummer": "22 24 90 90",
        "telefontype": "TFON",
        "bruksperiode": {
          "fom": "2014-05-21T21:07:05.975"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-17"
        }
      }
    ],
    "telefaksnummer": [
      {
        "nummer": "22 24 95 75",
        "telefontype": "TFAX",
        "bruksperiode": {
          "fom": "2014-05-21T21:07:05.975"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-17"
        }
      }
    ],
    "formaal": [
      {
        "formaal": "Sosialdepartementet har konstitusjonelt ansvar for trygd og sosiale\nytelser.",
        "bruksperiode": {
          "fom": "2014-05-22T00:09:46.195"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-17"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": true,
      "bruksperiode": {
        "fom": "2016-02-19T14:38:16.635"
      },
      "gyldighetsperiode": {
        "fom": "2016-02-19"
      }
    },
    "maalform": "NB",
    "sistEndret": "2017-05-29"
  },
  "juridiskEnhetDetaljer": {
    "enhetstype": "STAT",
    "sektorkode": "6100"
  }
}
"""

//language=JSON
private const val overenhetUtenEnDelFelter = """
   {
  "organisasjonsnummer": "313199770",
  "type": "JuridiskEnhet",
  "navn": {
    "sammensattnavn": "ALLSIDIG UTMERKET TIGER AS",
    "navnelinje1": "ALLSIDIG UTMERKET TIGER AS",
    "bruksperiode": {
      "fom": "2021-09-20T19:42:15.072"
    },
    "gyldighetsperiode": {
      "fom": "2021-03-18"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2021-03-17T00:00:00",
    "stiftelsesdato": "2000-05-17",
    "enhetstyper": [
      {
        "enhetstype": "AS",
        "bruksperiode": {
          "fom": "2021-09-20T19:42:15.071"
        },
        "gyldighetsperiode": {
          "fom": "2021-03-18"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "ALLSIDIG UTMERKET TIGER AS",
        "navnelinje1": "ALLSIDIG UTMERKET TIGER AS",
        "bruksperiode": {
          "fom": "2021-09-20T19:42:15.072"
        },
        "gyldighetsperiode": {
          "fom": "2021-03-18"
        }
      }
    ], 
    "ansatte": [
      {
        "antall": 2,
        "bruksperiode": {
          "fom": "2021-09-22T09:08:49.815"
        },
        "gyldighetsperiode": {
          "fom": "2021-03-18"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": false,
      "bruksperiode": {
        "fom": "1900-01-01T00:00:00"
      },
      "gyldighetsperiode": {
        "fom": "1900-01-01"
      }
    },
    "maalform": "NB",
    "sistEndret": "2021-03-18"
  },
  "juridiskEnhetDetaljer": {
    "enhetstype": "AS",
    "sektorkode": "2100",
    "kapitalopplysninger": [
      {
        "kapital": 1000000,
        "kapitalInnbetalt": 1000000,
        "valuta": "NOK",
        "fritekst": "null",
        "bruksperiode": {
          "fom": "2021-09-20T19:42:15.065"
        },
        "gyldighetsperiode": {
          "fom": "2021-03-18"
        }
      }
    ],
    "foretaksregisterRegistreringer": [
      {
        "registrert": true,
        "bruksperiode": {
          "fom": "2021-09-20T19:42:15.071"
        },
        "gyldighetsperiode": {
          "fom": "2021-03-18"
        }
      }
    ]
  }
}
"""

private const val underenhetIkkeFunnetRespons = """
{"melding": "Ingen organisasjon med organisasjonsnummer 910825674 ble funnet"}
"""

private const val overenhetIkkeFunnetRespons = """
{"timestamp":"2022-06-13T10:27:47.589+00:00","status":404,"error":"Not Found","path":"/v1/organisasjon/"}
"""

//language=JSON
private const val naviktRespons = """
{
  "organisasjonsnummer": "990983666",
  "type": "Virksomhet",
  "navn": {
    "sammensattnavn": "NAV IKT",
    "navnelinje1": "NAV IKT",
    "bruksperiode": {
      "fom": "2015-02-23T08:04:53.2"
    },
    "gyldighetsperiode": {
      "fom": "2010-04-09"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2007-03-05T00:00:00",
    "enhetstyper": [
      {
        "enhetstype": "BEDR",
        "bruksperiode": {
          "fom": "2014-05-21T15:46:47.225"
        },
        "gyldighetsperiode": {
          "fom": "2007-03-05"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "NAV IKT",
        "navnelinje1": "NAV IKT",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2010-04-09"
        }
      }
    ],
    "naeringer": [
      {
        "naeringskode": "84.300",
        "hjelpeenhet": false,
        "bruksperiode": {
          "fom": "2014-05-22T01:18:10.661"
        },
        "gyldighetsperiode": {
          "fom": "2006-07-01"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "Sannergata 2",
        "postnummer": "0557",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2007-08-23"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "Postboks 5 St Olavs plass",
        "postnummer": "0130",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2010-10-08"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": true,
      "bruksperiode": {
        "fom": "2015-01-27T16:01:18.562"
      },
      "gyldighetsperiode": {
        "fom": "2015-01-27"
      }
    },
    "sistEndret": "2014-02-17"
  },
  "virksomhetDetaljer": {
    "enhetstype": "BEDR",
    "oppstartsdato": "2006-07-01"
  },
  "bestaarAvOrganisasjonsledd": [
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "990983291",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "NAV IKT",
          "navnelinje1": "NAV IKT",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "2010-04-09"
          }
        },
        "organisasjonsleddOver": [
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "889640782",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
                "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2006-03-23"
                }
              },
              "inngaarIJuridiskEnheter": [
                {
                  "organisasjonsnummer": "983887457",
                  "navn": {
                    "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
                    "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
                    "bruksperiode": {
                      "fom": "2015-02-23T08:04:53.2"
                    },
                    "gyldighetsperiode": {
                      "fom": "2014-02-14"
                    }
                  },
                  "bruksperiode": {
                    "fom": "2014-05-23T15:42:14.826"
                  },
                  "gyldighetsperiode": {
                    "fom": "2006-03-23"
                  }
                }
              ]
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:42.452"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-05"
            }
          }
        ]
      },
      "bruksperiode": {
        "fom": "2014-05-23T20:17:42.64"
      },
      "gyldighetsperiode": {
        "fom": "2007-03-05"
      }
    }
  ]
}    
"""

//language=JSON
private const val velferdsEtatRespons = """
{
  "organisasjonsnummer": "889640782",
  "type": "Organisasjonsledd",
  "navn": {
    "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
    "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
    "bruksperiode": {
      "fom": "2015-02-23T08:04:53.2"
    },
    "gyldighetsperiode": {
      "fom": "2006-03-23"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2006-03-23T00:00:00",
    "stiftelsesdato": "2005-12-31",
    "enhetstyper": [
      {
        "enhetstype": "ORGL",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2006-03-23"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
        "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2006-03-23"
        }
      }
    ],
    "naeringer": [
      {
        "naeringskode": "84.120",
        "hjelpeenhet": false,
        "bruksperiode": {
          "fom": "2014-05-22T00:42:43.812"
        },
        "gyldighetsperiode": {
          "fom": "2005-12-31"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "Økernveien 94",
        "postnummer": "0579",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2014-07-13T04:02:31.517"
        },
        "gyldighetsperiode": {
          "fom": "2014-07-12"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "Postboks 5 St Olavs Plass",
        "postnummer": "0130",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2010-11-03"
        }
      }
    ],
    "internettadresser": [
      {
        "adresse": "www.nav.no",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:37.997"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "telefonnummer": [
      {
        "nummer": "21 07 00 00",
        "telefontype": "TFON",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "telefaksnummer": [
      {
        "nummer": "21 07 00 01",
        "telefontype": "TFAX",
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "formaal": [
      {
        "formaal": "Arbeids- og velferdsetaten har ansvaret for gjennomføringen av\narbeidsmarkeds- trygde- og pensjonspolitikken",
        "bruksperiode": {
          "fom": "2014-05-21T23:46:16.225"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "registrertMVA": [
      {
        "registrertIMVA": true,
        "bruksperiode": {
          "fom": "2014-05-21T18:52:38"
        },
        "gyldighetsperiode": {
          "fom": "2014-03-18"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": true,
      "bruksperiode": {
        "fom": "2014-12-29T09:43:08.386"
      },
      "gyldighetsperiode": {
        "fom": "2014-12-29"
      }
    },
    "maalform": "NB",
    "sistEndret": "2015-10-07"
  },
  "organisasjonsleddDetaljer": {
    "enhetstype": "ORGL",
    "sektorkode": "6100"
  },
  "driverVirksomheter": [
    {
      "organisasjonsnummer": "991003525",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN IKT DRIFT STEINKJER",
        "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
        "navnelinje3": "IKT DRIFT STEINKJER",
        "bruksperiode": {
          "fom": "2018-01-11T04:00:43.413"
        },
        "gyldighetsperiode": {
          "fom": "2018-01-10"
        }
      },
      "bruksperiode": {
        "fom": "2018-01-11T04:00:53.145"
      },
      "gyldighetsperiode": {
        "fom": "2018-01-10"
      }
    },
    {
      "organisasjonsnummer": "912998827",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD ØKERNVEIEN",
        "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
        "navnelinje3": "AVD ØKERNVEIEN",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2013-12-23"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T16:08:14.385"
      },
      "gyldighetsperiode": {
        "fom": "2013-12-23"
      }
    },
    {
      "organisasjonsnummer": "986001344",
      "navn": {
        "sammensattnavn": "NAV ABETAL",
        "navnelinje1": "NAV ABETAL",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2010-04-09"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T19:26:04.911"
      },
      "gyldighetsperiode": {
        "fom": "2010-04-12"
      }
    },
    {
      "organisasjonsnummer": "995298775",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG VELFERDSDIREKTORATET AVD SANNERGATA",
        "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
        "navnelinje3": "AVD SANNERGATA",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2010-03-16"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T21:01:47.564"
      },
      "gyldighetsperiode": {
        "fom": "2010-03-16"
      }
    }
  ],
  "inngaarIJuridiskEnheter": [
    {
      "organisasjonsnummer": "983887457",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-14"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T15:42:14.826"
      },
      "gyldighetsperiode": {
        "fom": "2006-03-23"
      }
    }
  ],
  "organisasjonsleddUnder": [
    {
      "organisasjonsledd": {
        "type": "Organisasjonsledd",
        "organisasjonsleddUnder": [
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "995277670",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV ØKONOMILINJEN",
                "navnelinje1": "NAV ØKONOMILINJEN",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2013-12-24"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T21:01:30.126"
            },
            "gyldighetsperiode": {
              "fom": "2010-03-11"
            }
          },
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "991012206",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV YTELSESLINJEN",
                "navnelinje1": "NAV YTELSESLINJEN",
                "bruksperiode": {
                  "fom": "2014-05-21T16:53:56.633"
                },
                "gyldighetsperiode": {
                  "fom": "2014-01-07"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:58.91"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-12"
            }
          },
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "991012133",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV ARBEIDS- OG TJENESTELINJEN",
                "navnelinje1": "NAV ARBEIDS- OG TJENESTELINJEN",
                "bruksperiode": {
                  "fom": "2016-02-02T04:02:14.967"
                },
                "gyldighetsperiode": {
                  "fom": "2016-02-01"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:58.879"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-12"
            }
          },
          {
            "organisasjonsledd": {
              "organisasjonsnummer": "990983291",
              "type": "Organisasjonsledd",
              "navn": {
                "sammensattnavn": "NAV IKT",
                "navnelinje1": "NAV IKT",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2010-04-09"
                }
              }
            },
            "bruksperiode": {
              "fom": "2014-05-23T20:17:42.452"
            },
            "gyldighetsperiode": {
              "fom": "2007-03-05"
            }
          }
        ]
      },
      "bruksperiode": {},
      "gyldighetsperiode": {}
    }
  ]
}
"""

//language=JSON
private const val sosDepRespons = """
{
  "organisasjonsnummer": "983887457",
  "type": "JuridiskEnhet",
  "navn": {
    "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
    "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
    "bruksperiode": {
      "fom": "2015-02-23T08:04:53.2"
    },
    "gyldighetsperiode": {
      "fom": "2014-02-14"
    }
  },
  "organisasjonDetaljer": {
    "registreringsdato": "2001-11-08T00:00:00",
    "enhetstyper": [
      {
        "enhetstype": "STAT",
        "bruksperiode": {
          "fom": "2014-05-21T21:07:05.975"
        },
        "gyldighetsperiode": {
          "fom": "2001-11-08"
        }
      }
    ],
    "navn": [
      {
        "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-14"
        }
      }
    ],
    "naeringer": [
      {
        "naeringskode": "84.110",
        "hjelpeenhet": false,
        "bruksperiode": {
          "fom": "2014-05-22T01:07:26.409"
        },
        "gyldighetsperiode": {
          "fom": "2001-11-08"
        }
      }
    ],
    "forretningsadresser": [
      {
        "type": "Forretningsadresse",
        "adresselinje1": "Akersgata 64",
        "postnummer": "0180",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2012-02-07"
        }
      }
    ],
    "postadresser": [
      {
        "type": "Postadresse",
        "adresselinje1": "Postboks 8019 Dep",
        "postnummer": "0030",
        "landkode": "NO",
        "kommunenummer": "0301",
        "bruksperiode": {
          "fom": "2015-02-23T10:38:34.403"
        },
        "gyldighetsperiode": {
          "fom": "2004-10-15"
        }
      }
    ],
    "epostadresser": [
      {
        "adresse": "postmottak@asd.dep.no",
        "bruksperiode": {
          "fom": "2016-08-02T04:03:15.626"
        },
        "gyldighetsperiode": {
          "fom": "2016-08-01"
        }
      }
    ],
    "internettadresser": [
      {
        "adresse": "regjeringen.no/asd",
        "bruksperiode": {
          "fom": "2016-08-02T04:03:15.641"
        },
        "gyldighetsperiode": {
          "fom": "2016-08-01"
        }
      }
    ],
    "telefonnummer": [
      {
        "nummer": "22 24 90 90",
        "telefontype": "TFON",
        "bruksperiode": {
          "fom": "2014-05-21T21:07:05.975"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-17"
        }
      }
    ],
    "telefaksnummer": [
      {
        "nummer": "22 24 95 75",
        "telefontype": "TFAX",
        "bruksperiode": {
          "fom": "2014-05-21T21:07:05.975"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-17"
        }
      }
    ],
    "formaal": [
      {
        "formaal": "Sosialdepartementet har konstitusjonelt ansvar for trygd og sosiale\nytelser.",
        "bruksperiode": {
          "fom": "2014-05-22T00:09:46.195"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-17"
        }
      }
    ],
    "navSpesifikkInformasjon": {
      "erIA": true,
      "bruksperiode": {
        "fom": "2016-02-19T14:38:16.635"
      },
      "gyldighetsperiode": {
        "fom": "2016-02-19"
      }
    },
    "maalform": "NB",
    "sistEndret": "2017-05-29"
  },
  "juridiskEnhetDetaljer": {
    "enhetstype": "STAT",
    "sektorkode": "6100"
  },
  "driverVirksomheter": [
    {
      "organisasjonsnummer": "983893449",
      "navn": {
        "sammensattnavn": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "navnelinje1": "ARBEIDS- OG SOSIALDEPARTEMENTET",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2014-02-14"
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T19:04:32.777"
      },
      "gyldighetsperiode": {
        "fom": "2001-11-08"
      }
    }
  ],
  "bestaarAvOrganisasjonsledd": [
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "982583462",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "STATENS PENSJONSKASSE FORVALTNINGSBEDRIFT",
          "navnelinje1": "STATENS PENSJONSKASSE",
          "navnelinje2": "FORVALTNINGSBEDRIFT",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "2001-04-27"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T18:50:12.687"
      },
      "gyldighetsperiode": {
        "fom": "2010-04-27"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "974761084",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "TRYGDERETTEN",
          "navnelinje1": "TRYGDERETTEN",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "1995-08-09"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T17:27:41.742"
      },
      "gyldighetsperiode": {
        "fom": "2007-03-08"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "889640782",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "ARBEIDS- OG VELFERDSETATEN",
          "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "2006-03-23"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T15:42:14.826"
      },
      "gyldighetsperiode": {
        "fom": "2006-03-23"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "971525681",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "ARBEIDSRETTEN",
          "navnelinje1": "ARBEIDSRETTEN",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "1995-08-09"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T16:19:26.171"
      },
      "gyldighetsperiode": {
        "fom": "2004-11-16"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "971524626",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "RIKSMEKLEREN",
          "navnelinje1": "RIKSMEKLEREN",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "2012-03-02"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T16:19:26.04"
      },
      "gyldighetsperiode": {
        "fom": "2004-11-16"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "986174613",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "PETROLEUMSTILSYNET",
          "navnelinje1": "PETROLEUMSTILSYNET",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "2003-10-22"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T19:27:51.325"
      },
      "gyldighetsperiode": {
        "fom": "2004-11-10"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "974761211",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "ARBEIDSTILSYNET",
          "navnelinje1": "ARBEIDSTILSYNET",
          "bruksperiode": {
            "fom": "2014-05-25T15:57:59.354"
          },
          "gyldighetsperiode": {
            "fom": "2014-04-24"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T17:27:41.865"
      },
      "gyldighetsperiode": {
        "fom": "2004-11-10"
      }
    },
    {
      "organisasjonsledd": {
        "organisasjonsnummer": "940415683",
        "type": "Organisasjonsledd",
        "navn": {
          "sammensattnavn": "PENSJONSTRYGDEN FOR SJØMENN",
          "navnelinje1": "PENSJONSTRYGDEN FOR SJØMENN",
          "bruksperiode": {
            "fom": "2015-02-23T08:04:53.2"
          },
          "gyldighetsperiode": {
            "fom": "1995-08-09"
          }
        }
      },
      "bruksperiode": {
        "fom": "2014-05-23T16:16:06.686"
      },
      "gyldighetsperiode": {
        "fom": "2002-07-25"
      }
    }
  ]
}
"""