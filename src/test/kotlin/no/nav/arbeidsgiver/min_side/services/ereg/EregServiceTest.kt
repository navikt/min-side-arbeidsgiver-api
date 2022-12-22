package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.tokenExchange.ClientAssertionTokenFactory
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
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

@MockBean(MultiIssuerConfiguration::class)
@RestClientTest(
    components = [EregService::class, EregCacheConfig::class],
)
class EregServiceTest {

    @MockBean
    lateinit var clientAssertionTokenFactory: ClientAssertionTokenFactory

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

        val result = eregService.hentUnderenhet(virksomhetsnummer)

        assertEquals("910825526", result.organizationNumber)
        assertEquals("GAMLE FREDRIKSTAD OG RAMNES REGNSKA P", result.name)
        assertEquals("810825472", result.parentOrganizationNumber)
        assertEquals("BEDR", result.organizationForm)
        assertEquals("Business", result.type)
        assertEquals("Active", result.status)
    }

    @Test
    fun `henter underenhet med orgledd fra ereg`() {
        val virksomhetsnummer = "42"
        server.expect(requestTo("/v1/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(underenhetMedOrgleddRespons, APPLICATION_JSON))

        val result = eregService.hentUnderenhet(virksomhetsnummer)

        assertEquals("912998827", result.organizationNumber)
        assertEquals("ARBEIDS- OG VELFERDSDIREKTORATET AVD FYRSTIKKALLÉEN", result.name)
        assertEquals("889640782", result.parentOrganizationNumber)
        assertEquals("BEDR", result.organizationForm)
        assertEquals("Business", result.type)
        assertEquals("Active", result.status)
    }

    @Test
    fun `underenhet er null fra ereg`() {
        val virksomhetsnummer = "42"
        server.expect(requestTo("/v1/organisasjon/$virksomhetsnummer?inkluderHierarki=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND).body(underenhetIkkeFunnetRespons).contentType(APPLICATION_JSON))

        val result = eregService.hentUnderenhet(virksomhetsnummer)

        assertEquals(null, result)
    }

    @Test
    fun `henter overenhet fra ereg`() {
        val orgnr = "314"
        server.expect(requestTo("/v1/organisasjon/$orgnr"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(overenhetRespons, APPLICATION_JSON))

        val result = eregService.hentOverenhet(orgnr)

        assertEquals("810825472", result.organizationNumber)
        assertEquals("MALMEFJORD OG RIDABU REGNSKAP", result.name)
        assertEquals(null, result.parentOrganizationNumber)
        assertEquals("AS", result.organizationForm)
        assertEquals("Enterprise", result.type)
        assertEquals("Active", result.status)
    }

    @Test
    fun `henter orgledd fra ereg`() {
        val orgnr = "314"
        server.expect(requestTo("/v1/organisasjon/$orgnr"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(orgleddRespons, APPLICATION_JSON))

        val result = eregService.hentOverenhet(orgnr)

        assertEquals("889640782", result.organizationNumber)
        assertEquals("ARBEIDS- OG VELFERDSETATEN", result.name)
        assertEquals(null, result.parentOrganizationNumber)
        assertEquals("ORGL", result.organizationForm)
        assertEquals("Enterprise", result.type)
        assertEquals("Active", result.status)
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

private val underenhetRespons = """
{
  "organisasjonsnummer": "910825526",
  "type": "Virksomhet",
  "navn": {
    "redigertnavn": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA",
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
        "redigertnavn": "GAMLE FREDRIKSTAD OG RAMNES REGNSKA",
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
        "redigertnavn": "MALMEFJORD OG RIDABU REGNSKAP",
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

private val overenhetRespons = """
{
  "organisasjonsnummer": "810825472",
  "type": "JuridiskEnhet",
  "navn": {
    "redigertnavn": "MALMEFJORD OG RIDABU REGNSKAP",
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
        "redigertnavn": "MALMEFJORD OG RIDABU REGNSKAP",
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

private val underenhetMedOrgleddRespons = """
{
  "organisasjonsnummer": "912998827",
  "type": "Virksomhet",
  "navn": {
    "redigertnavn": "ARBEIDS- OG VELFERDSDIREKTORATET",
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
        "redigertnavn": "ARBEIDS- OG VELFERDSDIREKTORATET",
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
          "redigertnavn": "ARBEIDS- OG VELFERDSETATEN",
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

private val orgleddRespons = """
{
  "organisasjonsnummer": "889640782",
  "type": "Organisasjonsledd",
  "navn": {
    "redigertnavn": "ARBEIDS- OG VELFERDSETATEN",
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
    "enhetstyper": [
      {
        "enhetstype": "ORGL",
        "bruksperiode": {
          "fom": "2020-04-28T04:01:22.192"
        },
        "gyldighetsperiode": {
          "fom": "2006-03-23"
        }
      }
    ],
    "navn": [
      {
        "redigertnavn": "ARBEIDS- OG VELFERDSETATEN",
        "navnelinje1": "ARBEIDS- OG VELFERDSETATEN",
        "bruksperiode": {
          "fom": "2015-02-23T08:04:53.2"
        },
        "gyldighetsperiode": {
          "fom": "2006-03-23"
        }
      }
    ]
  },
  "organisasjonsleddDetaljer": {
    "enhetstype": "ORGL",
    "sektorkode": "6100"
  }
}
"""

private val underenhetIkkeFunnetRespons = """
{"melding": "Ingen organisasjon med organisasjonsnummer 910825674 ble funnet"}
"""

private val overenhetIkkeFunnetRespons = """
{"timestamp":"2022-06-13T10:27:47.589+00:00","status":404,"error":"Not Found","path":"/v1/organisasjon/"}
"""
