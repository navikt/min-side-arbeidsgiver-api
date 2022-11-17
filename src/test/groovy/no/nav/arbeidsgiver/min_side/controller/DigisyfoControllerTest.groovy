package no.nav.arbeidsgiver.min_side.controller

import groovy.json.JsonSlurper
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykmeldingRepository
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
        value = DigisyfoController,
        properties = [
            "server.servlet.context-path=/",
            "tokensupport.enabled=false",
        ]
)
class DigisyfoControllerTest extends Specification {
    @SpringBean
    AuthenticatedUserHolder authenticatedUserHolder = Mock()

    @SpringBean
    NærmestelederRepository nærmestelederRepository = Mock()

    @SpringBean
    SykmeldingRepository sykmeldingRepository = Mock()

    def mkUnderenhet(String orgnr, String parentOrgnr) {
        return Organisasjon.builder()
                .name("underenhet")
                .organizationNumber(orgnr)
                .parentOrganizationNumber(parentOrgnr)
                .organizationForm("BEDR")
                .build()
    }
    def mkOverenhet(String orgnr) {
        return Organisasjon.builder()
                .name("overenhet")
                .organizationNumber(orgnr)
                .organizationForm("AS")
                .build()
    }


    def enhetsregisteret = [
            "1": mkOverenhet("1"),
            "10": mkUnderenhet("10", "1"),
            "2": mkOverenhet("2"),
            "20": mkUnderenhet("20", "2"),
            "3": mkOverenhet("3"),
            "30": mkUnderenhet("30", "3"),
    ]


    @SpringBean
    EregService eregService = Mock() {
        hentOverenhet(_ as String) >> { String orgnr ->
            return enhetsregisteret[orgnr]
        }
        hentUnderenhet(_ as String) >> { String orgnr ->
            return enhetsregisteret[orgnr]
        }
    }

    @Autowired
    MockMvc mockMvc
    def jsonSlurper = new JsonSlurper()


    def "Ingen rettigheter"() {
        given:
        authenticatedUserHolder.getFnr() >> "42"
        nærmestelederRepository.virksomheterSomNærmesteLeder("42") >> []
        sykmeldingRepository.oversiktSykmeldinger("42") >> [:]

        expect:
        def jsonResponse = mockMvc.perform(
                get("/api/narmesteleder/virksomheter-v2").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        jsonSlurper.parseText(jsonResponse) == jsonSlurper.parseText("[]")
    }

    def "Ingen rettigheter, selv om det finnes sykmeldinger i virksomheten"() {
        given:
        authenticatedUserHolder.getFnr() >> "42"
        nærmestelederRepository.virksomheterSomNærmesteLeder("42") >> []
        sykmeldingRepository.oversiktSykmeldinger("42") >> [
                "42": 1
        ]

        expect:
        def jsonResponse = mockMvc.perform(
                get("/api/narmesteleder/virksomheter-v2").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        jsonSlurper.parseText(jsonResponse) == jsonSlurper.parseText("[]")
    }

    def "Er nærmeste leder, med sykmeldinger registrert"() {
        given:
        authenticatedUserHolder.getFnr() >> "42"
        nærmestelederRepository.virksomheterSomNærmesteLeder("42") >> ["10", "20", "30"]
        sykmeldingRepository.oversiktSykmeldinger("42") >> ["10": 1, "20" : 2]
        sykmeldingRepository.oversiktSykmeldte("42") >> ["10": 1]
        expect:
        def jsonResponse = mockMvc.perform(
                get("/api/narmesteleder/virksomheter-v2").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        jsonSlurper.parseText(jsonResponse) == jsonSlurper.parseText("""
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
            "antallSykmeldinger": 1,
            "antallSykmeldte": 1
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
            "antallSykmeldinger": 0,
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
            "antallSykmeldinger": 2,
            "antallSykmeldte": 0
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
            "antallSykmeldinger": 0,
            "antallSykmeldte": 0
          }
        ]
""")
    }
}
