package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.ktor.server.plugins.di.*
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.kotlinCapture
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService.VirksomhetOgAntallSykmeldte
import no.nav.arbeidsgiver.min_side.services.ereg.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito

class DigisyfoServiceTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<DigisyfoRepository> { Mockito.mock<DigisyfoRepositoryImpl>() }
                provide<EregClient> { Mockito.mock<EregClient>() }
                provide<DigisyfoService>(DigisyfoService::class)
                provide<MeterRegistry> { Mockito.mock(MeterRegistry::class.java, Answers.RETURNS_DEEP_STUBS) }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

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
        orgnrCaptor = ArgumentCaptor.forClass(String::class.java)
        runBlocking {
            Mockito.`when`(
                app.getDependency<EregClient>().hentOverenhet(orgnrCaptor.kotlinCapture())
            ).thenAnswer {
                enhetsregisteret[orgnrCaptor.value]
            }

            Mockito.`when`(
                app.getDependency<EregClient>().hentUnderenhet(orgnrCaptor.kotlinCapture())
            ).thenAnswer {
                enhetsregisteret[orgnrCaptor.value]
            }
        }
    }

    @Test
    fun `Ingen rettigheter`() = app.runTest {
        Mockito.`when`(app.getDependency<DigisyfoRepository>().virksomheterOgSykmeldte("42")).thenReturn(listOf())
        val result = app.getDependency<DigisyfoService>().hentVirksomheterOgSykmeldte("42")
        assertThat(result).isEmpty()
    }

    @Test
    fun `noen rettigheter`() = app.runTest {
        Mockito.`when`(app.getDependency<DigisyfoRepository>().virksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoRepository.Virksomhetsinfo("10", 0),
                DigisyfoRepository.Virksomhetsinfo("11", 1),
                DigisyfoRepository.Virksomhetsinfo("20", 2),
            )
        )

        val result = app.getDependency<DigisyfoService>().hentVirksomheterOgSykmeldte("42")
        assertThat(result).containsExactly(
            VirksomhetOgAntallSykmeldte(
                orgnr = "1",
                navn = "overenhet",
                organisasjonsform = "AS",
                antallSykmeldte = 1,
                orgnrOverenhet = null,
                underenheter = mutableListOf(
                    VirksomhetOgAntallSykmeldte(
                        orgnr = "10",
                        navn = "underenhet",
                        organisasjonsform = "BEDR",
                        antallSykmeldte = 0,
                        orgnrOverenhet = "1",
                        underenheter = mutableListOf()
                    ),
                    VirksomhetOgAntallSykmeldte(
                        orgnr = "11",
                        navn = "underenhet",
                        organisasjonsform = "BEDR",
                        antallSykmeldte = 1,
                        orgnrOverenhet = "1",
                        underenheter = mutableListOf()
                    )
                )
            ),
            VirksomhetOgAntallSykmeldte(
                orgnr = "2",
                navn = "overenhet",
                organisasjonsform = "AS",
                antallSykmeldte = 2,
                orgnrOverenhet = null,
                underenheter = mutableListOf(
                    VirksomhetOgAntallSykmeldte(
                        orgnr = "20",
                        navn = "underenhet",
                        organisasjonsform = "BEDR",
                        antallSykmeldte = 2,
                        orgnrOverenhet = "2",
                        underenheter = mutableListOf()
                    )
                )
            )
        )
    }

    @Test
    fun `nestede rettigheter`() = app.runTest {
        Mockito.`when`(app.getDependency<DigisyfoRepository>().virksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoRepository.Virksomhetsinfo("3000", 2),
                DigisyfoRepository.Virksomhetsinfo("301", 1),
                DigisyfoRepository.Virksomhetsinfo("20", 1),
                DigisyfoRepository.Virksomhetsinfo("10", 1),
                DigisyfoRepository.Virksomhetsinfo("11", 1),
            )
        )

        val result = app.getDependency<DigisyfoService>().hentVirksomheterOgSykmeldte("42")
        assertThat(result).isEqualTo(
            listOf(
                VirksomhetOgAntallSykmeldte(
                    orgnr = "3",
                    navn = "overenhet",
                    organisasjonsform = "AS",
                    antallSykmeldte = 3,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        VirksomhetOgAntallSykmeldte(
                            orgnr = "30",
                            navn = "underenhet",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 3,
                            orgnrOverenhet = "3",
                            underenheter = mutableListOf(
                                VirksomhetOgAntallSykmeldte(
                                    orgnr = "300",
                                    navn = "underenhet",
                                    organisasjonsform = "BEDR",
                                    antallSykmeldte = 2,
                                    orgnrOverenhet = "30",
                                    underenheter = mutableListOf(
                                        VirksomhetOgAntallSykmeldte(
                                            orgnr = "3000",
                                            navn = "underenhet",
                                            organisasjonsform = "BEDR",
                                            antallSykmeldte = 2,
                                            orgnrOverenhet = "300",
                                            underenheter = mutableListOf()
                                        )
                                    )
                                ),
                                VirksomhetOgAntallSykmeldte(
                                    orgnr = "301",
                                    navn = "underenhet",
                                    organisasjonsform = "BEDR",
                                    antallSykmeldte = 1,
                                    orgnrOverenhet = "30",
                                    underenheter = mutableListOf()
                                )
                            )
                        )
                    )
                ),
                VirksomhetOgAntallSykmeldte(
                    orgnr = "2",
                    navn = "overenhet",
                    organisasjonsform = "AS",
                    antallSykmeldte = 1,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        VirksomhetOgAntallSykmeldte(
                            orgnr = "20",
                            navn = "underenhet",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 1,
                            orgnrOverenhet = "2",
                            underenheter = mutableListOf()
                        )
                    )
                ),
                VirksomhetOgAntallSykmeldte(
                    orgnr = "1",
                    navn = "overenhet",
                    organisasjonsform = "AS",
                    antallSykmeldte = 2,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        VirksomhetOgAntallSykmeldte(
                            orgnr = "10",
                            navn = "underenhet",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 1,
                            orgnrOverenhet = "1",
                            underenheter = mutableListOf()
                        ),
                        VirksomhetOgAntallSykmeldte(
                            orgnr = "11",
                            navn = "underenhet",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 1,
                            orgnrOverenhet = "1",
                            underenheter = mutableListOf()
                        )
                    )
                )
            )
        )
    }


    private fun mkUnderenhet(orgnr: String, parentOrgnr: String) =
        EregOrganisasjon(
            organisasjonsnummer = orgnr,
            organisasjonDetaljer = EregOrganisasjonDetaljer(
                ansatte = null,
                naeringer = null,
                enhetstyper = listOf(EregEnhetstype("BEDR", null)),
                postadresser = null,
                forretningsadresser = null,
                internettadresser = null
            ),
            inngaarIJuridiskEnheter = listOf(
                EregEnhetsRelasjon(
                    parentOrgnr, null
                )
            ),
            bestaarAvOrganisasjonsledd = null,
            type = "virksomhet",
            navn = EregNavn("underenhet", null)
        )

    private fun mkOverenhet(orgnr: String) =
        EregOrganisasjon(
            organisasjonsnummer = orgnr,
            organisasjonDetaljer = EregOrganisasjonDetaljer(
                ansatte = null,
                naeringer = null,
                enhetstyper = listOf(EregEnhetstype("AS", null)),
                postadresser = null,
                forretningsadresser = null,
                internettadresser = null
            ),
            inngaarIJuridiskEnheter = null,
            bestaarAvOrganisasjonsledd = null,
            type = "organisasjonsledd",
            navn = EregNavn("overenhet", null)
        )
}

