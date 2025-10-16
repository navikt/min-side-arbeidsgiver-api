package no.nav.arbeidsgiver.min_side.varslingstatus

import io.ktor.server.plugins.di.*
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.services.ereg.*
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient.Kontaktinfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class KontaktInfoPollingServiceTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<KontaktInfoPollingService>(KontaktInfoPollingService::class)
                provide<VarslingStatusRepository> { Mockito.mock<VarslingStatusRepository>() }
                provide<KontaktinfoClient> { Mockito.mock<KontaktinfoClient>() }
                provide<EregClient> { Mockito.mock<EregClient>() }
                provide<KontaktInfoPollerRepository> { Mockito.mock<KontaktInfoPollerRepository>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()

        val underenhetOrgnr = "1".repeat(9)

        val hovedenhetOrgnr = "2".repeat(9)

        val underenhet = EregOrganisasjon(
            organisasjonsnummer = underenhetOrgnr,
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
                    hovedenhetOrgnr, null
                )
            ),
            bestaarAvOrganisasjonsledd = null,
            type = "virksomhet",
            navn = EregNavn("organisasjon", null)
        )

        val ingenKontaktinfo = Kontaktinfo(
            eposter = setOf(),
            telefonnumre = setOf(),
        )

        val kontaktinfoMedEpost = Kontaktinfo(
            eposter = setOf("post@example.com"),
            telefonnumre = setOf(),
        )

        val kontaktinfoMedTlf = Kontaktinfo(
            eposter = setOf(),
            telefonnumre = setOf("00000000"),
        )
    }

    @BeforeEach
    fun setup() {
        runBlocking {
            Mockito.reset(
                app.getDependency<KontaktInfoPollerRepository>(),
                app.getDependency<KontaktinfoClient>(),
                app.getDependency<EregClient>(),
            )
        }
    }


    @Test
    fun `bruker kofuvi for underenhet om den finnes`() = app.runTest {
        // given
        `when`(app.getDependency<KontaktInfoPollerRepository>().getAndDeleteForPoll()).thenReturn(underenhetOrgnr)
        `when`(app.getDependency<EregClient>().hentUnderenhet(underenhetOrgnr)).thenReturn(underenhet)
        `when`(app.getDependency<KontaktinfoClient>().hentKontaktinfo(underenhetOrgnr)).thenReturn(kontaktinfoMedEpost)
        `when`(app.getDependency<KontaktinfoClient>().hentKontaktinfo(hovedenhetOrgnr)).thenReturn(kontaktinfoMedTlf)

        // when
        app.getDependency<KontaktInfoPollingService>().pollAndPullKontaktInfo()

        // then
        verify(app.getDependency<KontaktInfoPollerRepository>()).updateKontaktInfo(
            underenhetOrgnr,
            harEpost = true,
            harTlf = false,
        )
    }

    @Test
    fun `henter kofuvi for hovedenhet om det mangler kofuvi på underenhet`() = app.runTest {
        // given
        `when`(app.getDependency<KontaktInfoPollerRepository>().getAndDeleteForPoll()).thenReturn(underenhetOrgnr)
        `when`(app.getDependency<EregClient>().hentUnderenhet(underenhetOrgnr)).thenReturn(underenhet)
        `when`(app.getDependency<KontaktinfoClient>().hentKontaktinfo(underenhetOrgnr)).thenReturn(ingenKontaktinfo)
        `when`(app.getDependency<KontaktinfoClient>().hentKontaktinfo(hovedenhetOrgnr)).thenReturn(kontaktinfoMedEpost)

        // when
        app.getDependency<KontaktInfoPollingService>().pollAndPullKontaktInfo()

        // then
        verify(app.getDependency<KontaktInfoPollerRepository>()).updateKontaktInfo(
            underenhetOrgnr,
            harEpost = true,
            harTlf = false,
        )
    }
}