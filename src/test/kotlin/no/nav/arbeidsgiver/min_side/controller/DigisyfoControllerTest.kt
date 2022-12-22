package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykmeldingRepository
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [DigisyfoController::class],
    properties = ["server.servlet.context-path=/", "tokensupport.enabled=false"]
)
class DigisyfoControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @MockBean
    lateinit var nærmestelederRepository: NærmestelederRepository

    @MockBean
    lateinit var sykmeldingRepository: SykmeldingRepository

    @MockBean
    lateinit var eregService: EregService

    @MockBean
    lateinit var digisyfoService: DigisyfoService

    @Captor
    lateinit var orgnrCaptor: ArgumentCaptor<String>

    val enhetsregisteret = mapOf(
        "1" to mkOverenhet("1"),
        "10" to mkUnderenhet("10", "1"),
        "2" to mkOverenhet("2"),
        "20" to mkUnderenhet("20", "2"),
        "3" to mkOverenhet("3"),
        "30" to mkUnderenhet("30", "3"),
    )

    @BeforeEach
    fun setUp() {
        `when`(authenticatedUserHolder.fnr).thenReturn("42")

        `when`(
            eregService.hentOverenhet(orgnrCaptor.capture())
        ).thenAnswer {
            enhetsregisteret[orgnrCaptor.value]
        }

        `when`(
            eregService.hentUnderenhet(orgnrCaptor.capture())
        ).thenAnswer {
            enhetsregisteret[orgnrCaptor.value]
        }
    }

    @Test
    fun `Ingen rettigheter`() {
        `when`(nærmestelederRepository.virksomheterSomNærmesteLeder("42")).thenReturn(listOf())
        `when`(sykmeldingRepository.oversiktSykmeldinger("42")).thenReturn(mapOf())

        val jsonResponse = mockMvc
            .perform(get("/api/narmesteleder/virksomheter-v2").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        assertEquals("[]", jsonResponse)
    }

    @Test
    fun `Ingen rettigheter, selv om det finnes sykmeldinger i virksomheten`() {
        `when`(nærmestelederRepository.virksomheterSomNærmesteLeder("42")).thenReturn(listOf())
        `when`(sykmeldingRepository.oversiktSykmeldinger("42")).thenReturn(mapOf("42" to 1))

        val jsonResponse = mockMvc
            .perform(get("/api/narmesteleder/virksomheter-v2").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        assertEquals("[]", jsonResponse)
    }

    @Test
    fun `Er nærmeste leder, med sykmeldinger registrert`() {
        `when`(nærmestelederRepository.virksomheterSomNærmesteLeder("42")).thenReturn(listOf())
        `when`(digisyfoService.hentVirksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoController.VirksomhetOgAntallSykmeldte(mkUnderenhet("10", "1"), 0),
                DigisyfoController.VirksomhetOgAntallSykmeldte(mkOverenhet("1"), 0),
                DigisyfoController.VirksomhetOgAntallSykmeldte(mkUnderenhet("20", "2"), 1),
                DigisyfoController.VirksomhetOgAntallSykmeldte(mkOverenhet("2"), 0),
            )
        )

        val jsonResponse = mockMvc
            .perform(get("/api/narmesteleder/virksomheter-v3").accept(MediaType.APPLICATION_JSON))
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

    @Test
    fun `Er nærmeste leder, med sykmeldinger registrert, bakoverkompatibel`() {
        `when`(nærmestelederRepository.virksomheterSomNærmesteLeder("42")).thenReturn(listOf("10", "20", "30"))
        `when`(sykmeldingRepository.oversiktSykmeldinger("42")).thenReturn(mapOf("10" to 1, "20" to 2))

        val jsonResponse = mockMvc
            .perform(get("/api/narmesteleder/virksomheter-v2").accept(MediaType.APPLICATION_JSON))
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
                "antallSykmeldinger": 1
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
                "antallSykmeldinger": 0
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
                "antallSykmeldinger": 2
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
                "antallSykmeldinger": 0
              }
            ]
            """,
            jsonResponse,
            true
        )
    }

    private fun mkUnderenhet(orgnr: String, parentOrgnr: String) =
        Organisasjon.builder()
            .name("underenhet")
            .organizationNumber(orgnr)
            .parentOrganizationNumber(parentOrgnr)
            .organizationForm("BEDR")
            .build()

    private fun mkOverenhet(orgnr: String) =
        Organisasjon.builder()
            .name("overenhet")
            .organizationNumber(orgnr)
            .organizationForm("AS")
            .build()
}