package no.nav.arbeidsgiver.min_side.services.digisyfo

import no.nav.arbeidsgiver.min_side.controller.DigisyfoController
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [DigisyfoService.class])
class DigisyfoServiceTest extends Specification {
    @SpringBean
    DigisyfoRepositoryImpl digisyfoRepository = Mock()

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
            "11": mkUnderenhet("11", "1"),
            "2": mkOverenhet("2"),
            "20": mkUnderenhet("20", "2"),
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
    DigisyfoService digisyfoService = Mock()


    def "ingen rettigheter"() {
        given:
        digisyfoRepository.sykmeldtePrVirksomhet("42") >> []
        expect:
        digisyfoService.hentVirksomheterOgSykmeldte("42") == []
    }

    def "noen rettigheter"() {
        given:
        digisyfoRepository.sykmeldtePrVirksomhet("42") >> [
                new DigisyfoRepository.Virksomhetsinfo("10", 0),
                new DigisyfoRepository.Virksomhetsinfo("11", 1),
                new DigisyfoRepository.Virksomhetsinfo("20", 2),
        ]
        expect:
        digisyfoService.hentVirksomheterOgSykmeldte("42") == [
                new DigisyfoController.VirksomhetOgAntallSykmeldte(mkUnderenhet("10", "1"), 0),
                new DigisyfoController.VirksomhetOgAntallSykmeldte(mkUnderenhet("11", "1"), 1),
                new DigisyfoController.VirksomhetOgAntallSykmeldte(mkUnderenhet("20", "2"), 2),
                new DigisyfoController.VirksomhetOgAntallSykmeldte(mkOverenhet("1"), 0),
                new DigisyfoController.VirksomhetOgAntallSykmeldte(mkOverenhet("2"), 0),
        ]
    }
}
