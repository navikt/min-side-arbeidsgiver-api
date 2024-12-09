package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.min_side.kotlinCapture
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService.VirksomhetOgAntallSykmeldte
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@MockBean(MeterRegistry::class, answer = Answers.RETURNS_DEEP_STUBS)
@SpringBootTest(classes = [DigisyfoService::class])
class DigisyfoServiceTest {


    @MockBean
    lateinit var digisyfoRepository: DigisyfoRepositoryImpl

    @MockBean
    lateinit var eregService: EregService

    @Autowired
    lateinit var digisyfoService: DigisyfoService

    @Captor
    lateinit var orgnrCaptor: ArgumentCaptor<String>

    val enhetsregisteret = mapOf(
        "1" to mkOverenhet("1"),
        "10" to mkUnderenhet("10", "1"),
        "11" to mkUnderenhet("11", "1"),
        "2" to mkOverenhet("2"),
        "20" to mkUnderenhet("20", "2"),
        "3" to mkOverenhet("3"),
        "30" to mkUnderenhet("30", "3"),
        "300" to mkUnderenhet("300", "30"),
        "3000" to mkUnderenhet("3000", "300"),
        "301" to mkUnderenhet("301", "30"),
    )

    @BeforeEach
    fun setUp() {
        Mockito.`when`(
            eregService.hentOverenhet(orgnrCaptor.kotlinCapture())
        ).thenAnswer {
            enhetsregisteret[orgnrCaptor.value]
        }

        Mockito.`when`(
            eregService.hentUnderenhet(orgnrCaptor.kotlinCapture())
        ).thenAnswer {
            enhetsregisteret[orgnrCaptor.value]
        }
    }

    @Test
    fun `Ingen rettigheter`() {
        Mockito.`when`(digisyfoRepository.virksomheterOgSykmeldte("42")).thenReturn(listOf())

        val result = digisyfoService.hentVirksomheterOgSykmeldte("42")
        assertThat(result).isEmpty()
    }

    @Test
    fun `noen rettigheter`() {
        Mockito.`when`(digisyfoRepository.virksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoRepository.Virksomhetsinfo("10", 0),
                DigisyfoRepository.Virksomhetsinfo("11", 1),
                DigisyfoRepository.Virksomhetsinfo("20", 2),
            )
        )

        val result = digisyfoService.hentVirksomheterOgSykmeldte("42")
        assertThat(result).containsExactly(
            VirksomhetOgAntallSykmeldte(mkUnderenhet("10", "1"), 0),
            VirksomhetOgAntallSykmeldte(mkUnderenhet("11", "1"), 1),
            VirksomhetOgAntallSykmeldte(mkUnderenhet("20", "2"), 2),
            VirksomhetOgAntallSykmeldte(mkOverenhet("1"), 0),
            VirksomhetOgAntallSykmeldte(mkOverenhet("2"), 0),
        )
    }

    @Test
    fun `nestede rettigheter`() {
        Mockito.`when`(digisyfoRepository.virksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoRepository.Virksomhetsinfo("3000", 2),
                DigisyfoRepository.Virksomhetsinfo("301", 1),
            )
        )

        val result = digisyfoService.hentVirksomheterOgSykmeldte("42")
        assertThat(result).containsExactly(
            VirksomhetOgAntallSykmeldte(mkUnderenhet("3000", "300"), 2),
            VirksomhetOgAntallSykmeldte(mkUnderenhet("301", "30"), 1),
            VirksomhetOgAntallSykmeldte(mkUnderenhet("300", "30"), 0),
            VirksomhetOgAntallSykmeldte(mkUnderenhet("30", "3"), 0),
            VirksomhetOgAntallSykmeldte(mkOverenhet("3"), 0),
        )
    }

    @Test
    fun `nestede rettigheter hentVirksomheterOgSykmeldteV3`() {
        Mockito.`when`(digisyfoRepository.virksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoRepository.Virksomhetsinfo("3000", 2),
                DigisyfoRepository.Virksomhetsinfo("301", 1),
                DigisyfoRepository.Virksomhetsinfo("20", 1),
                DigisyfoRepository.Virksomhetsinfo("11", 1),
            )
        )

        val result = digisyfoService.hentVirksomheterOgSykmeldteV3("42")
        assertThat(result).isEqualTo(digisyfoVirksomheterHieraki)
    }


    private fun mkUnderenhet(orgnr: String, parentOrgnr: String) =
        Organisasjon(
            name = "underenhet",
            organizationNumber = orgnr,
            parentOrganizationNumber = parentOrgnr,
            organizationForm = "BEDR",
        )

    private fun mkOverenhet(orgnr: String) =
        Organisasjon(
            name = "overenhet",
            organizationNumber = orgnr,
            organizationForm = "AS",
        )
}

val digisyfoVirksomheterHieraki = listOf(
    DigisyfoService.VirksomhetOgAntallSykmeldteV3(
        orgnr = "3",
        navn = "overenhet",
        organisasjonsform = "AS",
        antallSykmeldte = 0,
        underenheter = listOf(
            DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                orgnr = "30",
                navn = "underenhet",
                organisasjonsform = "BEDR",
                antallSykmeldte = 0,
                underenheter = listOf(
                    DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                        orgnr = "301",
                        navn = "underenhet",
                        organisasjonsform = "BEDR",
                        antallSykmeldte = 1,
                        underenheter = null
                    ),
                    DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                        orgnr = "300",
                        navn = "underenhet",
                        organisasjonsform = "BEDR",
                        antallSykmeldte = 0,
                        underenheter = listOf(
                            DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                                orgnr = "3000",
                                navn = "underenhet",
                                organisasjonsform = "BEDR",
                                antallSykmeldte = 2,
                                underenheter = null
                            )
                        )
                    )
                )
            )
        )
    ),
    DigisyfoService.VirksomhetOgAntallSykmeldteV3(
        orgnr = "2",
        navn = "overenhet",
        organisasjonsform = "AS",
        antallSykmeldte = 0,
        underenheter = listOf(
            DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                orgnr = "20",
                navn = "underenhet",
                organisasjonsform = "BEDR",
                antallSykmeldte = 1,
                underenheter = null
            )
        )
    ),
    DigisyfoService.VirksomhetOgAntallSykmeldteV3(
        orgnr = "1",
        navn = "overenhet",
        organisasjonsform = "AS",
        antallSykmeldte = 0,
        underenheter = listOf(
            DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                orgnr = "10",
                navn = "underenhet",
                organisasjonsform = "BEDR",
                antallSykmeldte = 1,
                underenheter = null
            ),
            DigisyfoService.VirksomhetOgAntallSykmeldteV3(
                orgnr = "11",
                navn = "underenhet",
                organisasjonsform = "BEDR",
                antallSykmeldte = 1,
                underenheter = null
            )
        )
    )
)