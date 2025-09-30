package no.nav.arbeidsgiver.min_side.kontaktinfo

import io.ktor.server.plugins.di.*
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.kotlinAny
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import no.nav.arbeidsgiver.min_side.services.ereg.*
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService.KontaktinfoRequest
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class KontaktinfoControllerAuthzTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<KontaktinfoClient> { Mockito.mock<KontaktinfoClient>() }
                provide<AltinnRollerClient> { Mockito.mock<AltinnRollerClient>() }
                provide<EregClient> { Mockito.mock<EregClient>() }
                provide<KontaktInfoService>(KontaktInfoService::class)
                provide<MaskinportenTokenService>(MaskinportenTokenServiceStub::class)
            }
        }
    }

    val authenticatedUserHolder = Mockito.mock<AuthenticatedUserHolder>()

    private val fnr = "012345678"
    private val orgnrUnderenhet = "0".repeat(9)
    private val orgnrHovedenhet = "1".repeat(9)
    private val orgnrAnnet = "2".repeat(9)


    @Test
    fun `tilgang til både underenhet og hovedenhet`() = app.runTest {
        mockTilganger(underenhet = true, hovedenhet = true)
        val kontakinfo =
            app.getDependency<KontaktInfoService>()
                .getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet), authenticatedUserHolder)
        assertNotNull(kontakinfo.hovedenhet)
        assertNotNull(kontakinfo.underenhet)
    }

    @Test
    fun `tilgang til kun underenhet`() = app.runTest {
        mockTilganger(underenhet = true, hovedenhet = false)

        val kontakinfo =
            app.getDependency<KontaktInfoService>()
                .getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet), authenticatedUserHolder)
        assertNull(kontakinfo.hovedenhet)
        assertNotNull(kontakinfo.underenhet)
    }

    @Test
    fun `tilgang til kun hovedenhet`() = app.runTest {
        mockTilganger(underenhet = false, hovedenhet = true)

        val kontakinfo =
            app.getDependency<KontaktInfoService>()
                .getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet), authenticatedUserHolder)
        assertNotNull(kontakinfo.hovedenhet)
        assertNull(kontakinfo.underenhet)
    }

    @Test
    fun `ikke tilgang til hverken hovedenhet eller underenhet `() = app.runTest {
        mockTilganger(underenhet = false, hovedenhet = false)

        val kontakinfo =
            app.getDependency<KontaktInfoService>()
                .getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet), authenticatedUserHolder)
        assertNull(kontakinfo.hovedenhet)
        assertNull(kontakinfo.underenhet)
    }

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            /* også kall med andre orgnr er vellykkede, for å unngå early return i controlleren, som kunne ha
             * skjult manglende tilgangssjekker. */
            val eregClient = app.getDependency<EregClient>()
            `when`(eregClient.hentUnderenhet(kotlinAny())).thenAnswer {
                if (it.arguments[0] == orgnrUnderenhet) {
                    EregOrganisasjon(
                        organisasjonsnummer = orgnrUnderenhet,
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
                                orgnrHovedenhet, null
                            )
                        ),
                        bestaarAvOrganisasjonsledd = null,
                        type = "virksomhet",
                        navn = EregNavn("organisasjon", null)
                    )
                } else {
                    EregOrganisasjon(
                        organisasjonsnummer = orgnrAnnet,
                        organisasjonDetaljer = EregOrganisasjonDetaljer(
                            ansatte = null,
                            naeringer = null,
                            enhetstyper = listOf(EregEnhetstype("BEDR", null)),
                            postadresser = null,
                            forretningsadresser = null,
                            internettadresser = null
                        ),
                        inngaarIJuridiskEnheter = null,
                        bestaarAvOrganisasjonsledd = null,
                        type = "organisasjonsledd",
                        navn = EregNavn("organisasjon", null)
                    )
                }
            }

            /* Returner alltid kontaktinfo, uavhengig av orgnr, så vi ikke skjuler feil. */
            val kontaktInfoClient = app.getDependency<KontaktinfoClient>()
            `when`(kontaktInfoClient.hentKontaktinfo(kotlinAny())).thenReturn(
                KontaktinfoClient.Kontaktinfo(setOf("x"), setOf("y"))
            )

            `when`(authenticatedUserHolder.fnr).thenReturn(fnr)
        }
    }

    /* Mock alle andre tilgangssjekker som true, for å provosere fram lekkasje. */
    private suspend fun mockTilganger(underenhet: Boolean, hovedenhet: Boolean) {
        val altinnRollerClient = app.getDependency<AltinnRollerClient>()
        `when`(altinnRollerClient.harAltinnRolle(kotlinAny(), kotlinAny(), kotlinAny(), kotlinAny())).thenAnswer {
            when (it.arguments[0]) {
                fnr -> when (it.arguments[1]) {
                    orgnrUnderenhet -> underenhet
                    orgnrHovedenhet -> hovedenhet
                    else -> true
                }

                else -> true
            }
        }
    }
}
