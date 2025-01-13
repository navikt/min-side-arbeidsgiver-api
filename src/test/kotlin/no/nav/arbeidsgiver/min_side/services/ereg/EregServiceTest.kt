package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.tokenExchange.ClientAssertionTokenFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@MockBean(ClientAssertionTokenFactory::class)
@RestClientTest(
    components = [EregService::class, EregCacheConfig::class],
)
class EregServiceTest {

    @Autowired
    lateinit var eregService: EregService

    @Autowired
    lateinit var server: MockRestServiceServer

    @Test
    fun `henter underenhet fra ereg`() {
        val virksomhetsnummer = "42"
        server.expect(requestTo("/v1/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(underenhetRespons, APPLICATION_JSON))

        val result = eregService.hentUnderenhet(virksomhetsnummer)!!

        assertEquals("910825526", result.organisasjonsnummer)
        assertEquals("GAMLE FREDRIKSTAD OG RAMNES REGNSKA P", result.navn)
        assertEquals("810825472", result.overordnetEnhet)
        assertEquals("BEDR", result.organisasjonsform)
        assertEquals(123, result.antallAnsatte)
        assertEquals(
            Adresse(
                type = "Postadresse",
                adresse = "PERSONALKONTORET, PHILIP LUNDQUIST,POSTBOKS 144",
                kommune = "",
                kommunenummer = "1120",
                land = "",
                landkode = "NO",
                poststed = "KLEPPE",
                postnummer = "4358"
            ), result.postadresse
        )
        assertEquals(
            Adresse(
                type = "Forretningsadresse",
                adresse = "AVDELING HORTEN, VED PHILIP LUNDQUIST, APOTEKERGATA 16",
                kommune = "",
                kommunenummer = "3801",
                land = "",
                landkode = "NO",
                poststed = "HORTEN",
                postnummer = "3187"
            ), result.forretningsadresse
        )
        assertEquals("", result.hjemmeside)
        assertEquals(emptyList<Kode>(), result.naeringskoder)
        assertEquals(true, result.harRegistrertAntallAnsatte)
    }

    @Test
    fun `henter underenhet med orgledd fra ereg`() {
        val virksomhetsnummer = "42"
        server.expect(requestTo("/v1/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(underenhetMedOrgleddRespons, APPLICATION_JSON))

        val result = eregService.hentUnderenhet(virksomhetsnummer)!!

        assertEquals("912998827", result.organisasjonsnummer)
        assertEquals("ARBEIDS- OG VELFERDSDIREKTORATET AVD FYRSTIKKALLÉEN", result.navn)
        assertEquals("889640782", result.overordnetEnhet)
        assertEquals("BEDR", result.organisasjonsform)
        assertEquals(null, result.antallAnsatte)
        assertEquals(null, result.postadresse)
        assertEquals(null, result.forretningsadresse)
        assertEquals("", result.hjemmeside)
        assertEquals(emptyList<Kode>(), result.naeringskoder)
        assertEquals(false, result.harRegistrertAntallAnsatte)
    }

    @Test
    fun `underenhet er null fra ereg`() {
        val virksomhetsnummer = "42"
        server.expect(requestTo("/v1/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND).body(underenhetIkkeFunnetRespons).contentType(APPLICATION_JSON)
            )

        val result = eregService.hentUnderenhet(virksomhetsnummer)

        assertEquals(null, result)
    }

    @Test
    fun `henter overenhet fra ereg`() {
        val orgnr = "314"
        server.expect(requestTo("/v1/organisasjon/$orgnr"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(overenhetRespons, APPLICATION_JSON))

        val result = eregService.hentOverenhet(orgnr)!!

        assertEquals("810825472", result.organisasjonsnummer)
        assertEquals("MALMEFJORD OG RIDABU REGNSKAP", result.navn)
        assertEquals(null, result.overordnetEnhet)
        assertEquals("AS", result.organisasjonsform)
        assertEquals(null, result.antallAnsatte)
        assertEquals(
            Adresse(
                type = "Postadresse",
                adresse = "POSTBOKS 4120",
                kommune = "",
                kommunenummer = "3403",
                land = "",
                landkode = "NO",
                poststed = "HAMAR",
                postnummer = "2307"
            ), result.postadresse
        )
        assertEquals(
            Adresse(
                type = "Forretningsadresse",
                adresse = "RÅDHUSET",
                kommune = "",
                kommunenummer = "1579",
                land = "",
                landkode = "NO",
                poststed = "ELNESVÅGEN",
                postnummer = "6440"
            ), result.forretningsadresse
        )
        assertEquals("", result.hjemmeside)
        assertEquals(emptyList<Kode>(), result.naeringskoder)
        assertEquals(false, result.harRegistrertAntallAnsatte)
    }

    @Test
    fun `henter orgledd fra ereg`() {
        val orgnr = "314"
        server.expect(requestTo("/v1/organisasjon/$orgnr"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(orgleddRespons, APPLICATION_JSON))

        val result = eregService.hentOverenhet(orgnr)!!

        assertEquals("889640782", result.organisasjonsnummer)
        assertEquals("ARBEIDS- OG VELFERDSETATEN", result.navn)
        assertEquals("983887457", result.overordnetEnhet)
        assertEquals("ORGL", result.organisasjonsform)
        assertEquals(null, result.antallAnsatte)
        assertEquals(
            Adresse(
                type = "Postadresse",
                adresse = "Postboks 5 St Olavs Plass",
                kommune = "",
                kommunenummer = "0301",
                land = "",
                landkode = "NO",
                poststed = "",
                postnummer = "0130"
            ), result.postadresse
        )
        assertEquals(
            Adresse(
                type = "Forretningsadresse",
                adresse = "Økernveien 94",
                kommune = "",
                kommunenummer = "0301",
                land = "",
                landkode = "NO",
                poststed = "",
                postnummer = "0579"
            ), result.forretningsadresse
        )
        assertEquals("www.nav.no", result.hjemmeside)
        assertEquals(listOf(Kode("84.120", "")), result.naeringskoder)
        assertEquals(false, result.harRegistrertAntallAnsatte)
    }

    @Test
    fun `henter jurudisk enhet for orgledd 889640782`() {
        val orgnr = "314"
        server.expect(requestTo("/v1/organisasjon/$orgnr"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(juridiskEnhetForOrgleddRespons, APPLICATION_JSON))

        val result = eregService.hentOverenhet(orgnr)!!

        assertEquals("983887457", result.organisasjonsnummer)
        assertEquals("ARBEIDS- OG SOSIALDEPARTEMENTET", result.navn)
        assertEquals(null, result.overordnetEnhet)
        assertEquals("STAT", result.organisasjonsform)
        assertEquals(
            Adresse(
                type = "Postadresse",
                adresse = "Postboks 8019 Dep",
                kommune = "",
                kommunenummer = "0301",
                land = "",
                landkode = "NO",
                poststed = "",
                postnummer = "0030"
            ), result.postadresse
        )
        assertEquals(
            Adresse(
                type = "Forretningsadresse",
                adresse = "Akersgata 64",
                kommune = "",
                kommunenummer = "0301",
                land = "",
                landkode = "NO",
                poststed = "",
                postnummer = "0180"
            ), result.forretningsadresse
        )
        assertEquals("regjeringen.no/asd", result.hjemmeside)
        assertEquals(listOf(Kode("84.110", "")), result.naeringskoder)
        assertEquals(false, result.harRegistrertAntallAnsatte)
    }

    @Test
    fun `overenhet er null fra ereg`() {
        val orgnr = "314"
        server.expect(requestTo("/v1/organisasjon/$orgnr"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND).body(overenhetIkkeFunnetRespons).contentType(APPLICATION_JSON))

        val result = eregService.hentOverenhet(orgnr)

        assertEquals(null, result)
    }
}

//Responsene er hentet fra https://ereg-services.dev.intern.nav.no/swagger-ui/index.html#/organisasjon.v1/hentOrganisasjonUsingGET

private const val underenhetRespons = """
{
  "organisasjonsnummer": "910825526",
  "type": "Virksomhet",
  "navn": {
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
        "antall": 123,
        "bruksperiode": {
          "fom": "2015-01-06T21:44:04.748",
          "tom": "2015-12-06T19:45:04"
        },
        "gyldighetsperiode": {
          "fom": "2014-07-01",
          "tom": "2015-12-31"
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

private const val overenhetRespons = """
{
  "organisasjonsnummer": "810825472",
  "type": "JuridiskEnhet",
  "navn": {
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
  }
}
"""

private const val underenhetMedOrgleddRespons = """
{
  "organisasjonsnummer": "912998827",
  "type": "Virksomhet",
  "navn": {
    "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
    "navnelinje3": "AVD FYRSTIKKALLÉEN",
    "bruksperiode": {
      "fom": "2020-08-12T04:01:10.282"
    },
    "gyldighetsperiode": {
      "fom": "2020-08-11"
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
        "navnelinje1": "ARBEIDS- OG VELFERDSDIREKTORATET",
        "navnelinje3": "AVD FYRSTIKKALLÉEN",
        "bruksperiode": {
          "fom": "2020-08-12T04:01:10.282"
        },
        "gyldighetsperiode": {
          "fom": "2020-08-11"
        }
      }
    ]
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
        "fom": "2014-05-23T16:08:14.385"
      },
      "gyldighetsperiode": {
        "fom": "2013-12-23"
      }
    }
  ]
}
"""

private const val orgleddRespons = """
{
  "organisasjonsnummer": "889640782",
  "type": "Organisasjonsledd",
  "navn": {
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

private const val juridiskEnhetForOrgleddRespons = """
 {
  "organisasjonsnummer": "983887457",
  "type": "JuridiskEnhet",
  "navn": {
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

private const val underenhetIkkeFunnetRespons = """
{"melding": "Ingen organisasjon med organisasjonsnummer 910825674 ble funnet"}
"""

private const val overenhetIkkeFunnetRespons = """
{"timestamp":"2022-06-13T10:27:47.589+00:00","status":404,"error":"Not Found","path":"/v1/organisasjon/"}
"""
