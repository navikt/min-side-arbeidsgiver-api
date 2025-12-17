package no.nav.arbeidsgiver.min_side.services.digisyfo

import kotlinx.coroutines.test.runTest
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoRepository.Virksomhetsinfo
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService.VirksomhetOgAntallSykmeldte
import no.nav.arbeidsgiver.min_side.services.ereg.*
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigisyfoServiceTest {

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

    val eregClientMock = object : EregClient {
        override suspend fun hentOrganisasjon(orgnummer: String): EregOrganisasjon? {
            return enhetsregisteret[orgnummer]
        }
    }


    @Test
    fun `Ingen rettigheter`() {
        runTest {
            val service = DigisyfoServiceImpl(
                DigisyfoRepositoryMock(
                    mapOf("42" to emptyList())
                ),
                eregClientMock,
            )
            val result = service.hentVirksomheterOgSykmeldte("42")
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun `noen rettigheter`() = runTest {
        val service = DigisyfoServiceImpl(
            DigisyfoRepositoryMock(
                mapOf(
                    "42" to listOf(
                        Virksomhetsinfo("10", 0),
                        Virksomhetsinfo("11", 1),
                        Virksomhetsinfo("20", 2),
                    )
                )
            ),
            eregClientMock,
        )

        val result = service.hentVirksomheterOgSykmeldte("42")
        assertEquals(
            listOf(
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
            ),
            result
        )
    }

    @Test
    fun `nestede rettigheter`() = runTest {
//        Mockito.`when`(app.getDependency<DigisyfoRepository>().virksomheterOgSykmeldte("42")).thenReturn(

//        )

//        val result = app.getDependency<DigisyfoService>().hentVirksomheterOgSykmeldte("42")
//        assertThat(result).isEqualTo(
//        )
        val service = DigisyfoServiceImpl(
            DigisyfoRepositoryMock(
                mapOf(
                    "42" to listOf(
                        Virksomhetsinfo("3000", 2),
                        Virksomhetsinfo("301", 1),
                        Virksomhetsinfo("20", 1),
                        Virksomhetsinfo("10", 1),
                        Virksomhetsinfo("11", 1),
                    )
                )
            ),
            eregClientMock,
        )
        val result = service.hentVirksomheterOgSykmeldte("42")
        assertEquals(
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
            ),
            result,
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

private class DigisyfoRepositoryMock(val mock: Map<String, List<Virksomhetsinfo>>) : DigisyfoRepository {
    override suspend fun virksomheterOgSykmeldte(nærmestelederFnr: String) =
        mock[nærmestelederFnr] ?: emptyList()


    override suspend fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse) =
        TODO("Not yet implemented")

    override suspend fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>) =
        TODO("Not yet implemented")

    override suspend fun deleteOldSykmelding(today: LocalDate) =
        TODO("Not yet implemented")

}

