package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.tokenExchange.ClientAssertionTokenFactory
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.spockframework.spring.SpringBean
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.util.DefaultUriBuilderFactory
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@StubBeans([MultiIssuerConfiguration])
@RestClientTest(
        components = [EregService, EregCacheConfig],
        properties = []
)
@AutoConfigureWebClient
class EregServiceTest extends Specification {
    @SpringBean
    ClientAssertionTokenFactory clientAssertionTokenFactory = Mock()

    @Autowired
    EregService eregService

    @Autowired
    MockRestServiceServer server

    DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory()


    def "henter underenhet fra ereg"() {
        given:
        def virksomhetsnummer = "42"
        server
                .expect(requestTo(uriBuilderFactory.expand(EregService.API_URL, virksomhetsnummer)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(underenhetRespons, APPLICATION_JSON))

        when:
        def result = eregService.hentUnderenhet(virksomhetsnummer)

        then:
        result.organisasjonsnummer == "776655441"
        result.navn == "SESAM STASJON"
        result.overordnetEnhet == "112233445"
    }

    def "hent slettet underenhet fra ereg returnerer null"() {
        given:
        def virksomhetsnummer = "42"
        server
                .expect(requestTo(uriBuilderFactory.expand(EregService.API_URL, virksomhetsnummer)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(slettetUnderenhetRespons, APPLICATION_JSON))

        when:
        def result = eregService.hentUnderenhet(virksomhetsnummer)

        then:
        result == null
    }

    def "hent fjernet underenhet fra ereg returnerer null"() {
        given:
        def virksomhetsnummer = "42"
        server
                .expect(requestTo(uriBuilderFactory.expand(EregService.API_URL, virksomhetsnummer)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(fjernetUnderenhetRespons, APPLICATION_JSON))

        when:
        def result = eregService.hentUnderenhet(virksomhetsnummer)

        then:
        result == null
    }

    def "404 NOT_FOUND propageres som returnerer null"() {
        given:
        def virksomhetsnummer = "42"
        server
                .expect(requestTo(uriBuilderFactory.expand(EregService.API_URL, virksomhetsnummer)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND))

        when:
        def result = eregService.hentUnderenhet(virksomhetsnummer)

        then:
        result == null
    }

    def underenhetRespons = """
{
  "organisasjonsnummer" : "776655441",
  "navn" : "SESAM STASJON",
  "organisasjonsform" : {
    "kode" : "BEDR",
    "beskrivelse" : "Bedrift",
    "_links" : {
      "self" : {
        "href" : "http://localhost/enhetsregisteret/api/organisasjonsformer/BEDR"
      }
    }
  },
  "postadresse" : {
    "land" : "Norge",
    "landkode" : "NO",
    "postnummer" : "0122",
    "poststed" : "OSLO",
    "adresse" : [ "c/o reder K. Rusing", "Postboks 1752 Vika", "" ],
    "kommune" : "OSLO",
    "kommunenummer" : "0301"
  },
  "registreringsdatoEnhetsregisteret" : "2017-10-20",
  "registrertIMvaregisteret" : true,
  "naeringskode1" : {
    "beskrivelse" : "Skipsmegling",
    "kode" : "52.292"
  },
  "antallAnsatte" : 50,
  "overordnetEnhet" : "112233445",
  "beliggenhetsadresse" : {
    "land" : "Norge",
    "landkode" : "NO",
    "postnummer" : "0122",
    "poststed" : "OSLO",
    "adresse" : [ "Tyvholmen 1", null, null ],
    "kommune" : "OSLO",
    "kommunenummer" : "0301"
  },
  "nedleggelsesdato" : "2018-10-20",
  "_links" : {
    "self" : {
      "href" : "http://localhost/enhetsregisteret/api/underenheter/776655441"
    },
    "overordnetEnhet" : {
      "href" : "http://localhost/enhetsregisteret/api/enheter/112233445"
    }
  }
}
"""

    def slettetUnderenhetRespons = """
{
  "organisasjonsnummer" : "987123456",
  "navn" : "SLETTET UNDERENHET AS",
  "organisasjonsform" : {
    "kode" : "AAFY",
    "beskrivelse" : "Virksomhet til ikke-næringsdrivende person",
    "_links" : {
      "self" : {
        "href" : "http://localhost/enhetsregisteret/api/organisasjonsformer/AAFY"
      }
    }
  },
  "slettedato" : "2017-10-20",
  "nedleggelsesdato" : "2017-10-5",
  "_links" : {
    "self" : {
      "href" : "http://localhost/enhetsregisteret/api/underenheter/987123456"
    }
  }
}
"""

    def fjernetUnderenhetRespons = """
{
  "organisasjonsnummer" : "123456780",
  "slettedato" : "2013-05-31",
  "_links" : {
    "self" : {
      "href" : "http://localhost/enhetsregisteret/api/underenheter/123456780"
    }
  }
}
"""

}

