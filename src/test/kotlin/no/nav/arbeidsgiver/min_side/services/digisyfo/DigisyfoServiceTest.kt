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