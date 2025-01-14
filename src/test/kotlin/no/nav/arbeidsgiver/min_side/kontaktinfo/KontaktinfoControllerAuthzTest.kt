package no.nav.arbeidsgiver.min_side.kontaktinfo

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.kontaktinfo.KontaktinfoController.KontaktinfoRequest
import no.nav.arbeidsgiver.min_side.kotlinAny
import no.nav.arbeidsgiver.min_side.services.ereg.EregEnhetsRelasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregEnhetstype
import no.nav.arbeidsgiver.min_side.services.ereg.EregNavn
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjonDetaljer
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(classes = [KontaktinfoController::class])
@MockBean(AuthenticatedUserHolder::class)
@MockBean(AltinnRollerClient::class)
@MockBean(EregService::class)
@MockBean(KontaktinfoClient::class)
class KontaktinfoControllerAuthzTest {
    @Autowired
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @Autowired
    lateinit var altinnRollerClient: AltinnRollerClient

    @Autowired
    lateinit var eregService: EregService

    @Autowired
    lateinit var kontaktinfoClient: KontaktinfoClient

    @Autowired
    lateinit var kontaktinfoController: KontaktinfoController

    private val fnr = "012345678"
    private val orgnrUnderenhet = "0".repeat(9)
    private val orgnrHovedenhet = "1".repeat(9)
    private val orgnrAnnet = "2".repeat(9)

    @Test
    fun `tilgang til både underenhet og hovedenhet`() {
        mockTilganger(underenhet = true, hovedenhet = true)

        val kontakinfo = kontaktinfoController.getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet))
        assertNotNull(kontakinfo.hovedenhet)
        assertNotNull(kontakinfo.underenhet)
    }

    @Test
    fun `tilgang til kun underenhet`() {
        mockTilganger(underenhet = true, hovedenhet = false)

        val kontakinfo = kontaktinfoController.getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet))
        assertNull(kontakinfo.hovedenhet)
        assertNotNull(kontakinfo.underenhet)
    }

    @Test
    fun `tilgang til kun hovedenhet`() {
        mockTilganger(underenhet = false, hovedenhet = true)

        val kontakinfo = kontaktinfoController.getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet))
        assertNotNull(kontakinfo.hovedenhet)
        assertNull(kontakinfo.underenhet)
    }

    @Test
    fun `ikke tilgang til hverken hovedenhet eller underenhet `() {
        mockTilganger(underenhet = false, hovedenhet = false)

        val kontakinfo = kontaktinfoController.getKontaktinfo(KontaktinfoRequest(orgnrUnderenhet))
        assertNull(kontakinfo.hovedenhet)
        assertNull(kontakinfo.underenhet)
    }

    @BeforeEach
    fun beforeEach() {
        /* også kall med andre orgnr er vellykkede, for å unngå early return i controlleren, som kunne ha
         * skjult manglende tilgangssjekker. */
        `when`(eregService.hentUnderenhet(kotlinAny())).thenAnswer {
            if (it.arguments[0] == orgnrUnderenhet) {
                EregOrganisasjon(
                    organisasjonsnummer = orgnrUnderenhet,
                    organisasjonsDetaljer = EregOrganisasjonDetaljer(
                        ansatte = null,
                        naeringer = null,
                        enhetstyper = listOf(EregEnhetstype("BEDR", null)),
                        postadresser = null,
                        forretningsadresser = null,
                        internettadresser = null
                    ),
                    ingaarIJuridiskEnheter = listOf(
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
                    organisasjonsDetaljer = EregOrganisasjonDetaljer(
                        ansatte = null,
                        naeringer = null,
                        enhetstyper = listOf(EregEnhetstype("BEDR", null)),
                        postadresser = null,
                        forretningsadresser = null,
                        internettadresser = null
                    ),
                    ingaarIJuridiskEnheter = null,
                    bestaarAvOrganisasjonsledd = null,
                    type = "organisasjonsledd",
                    navn = EregNavn("organisasjon", null)
                )
            }
        }

        /* Returner alltid kontaktinfo, uavhengig av orgnr, så vi ikke skjuler feil. */
        `when`(kontaktinfoClient.hentKontaktinfo(kotlinAny())).thenReturn(
            KontaktinfoClient.Kontaktinfo(setOf("x"), setOf("y"))
        )

        `when`(authenticatedUserHolder.fnr).thenReturn(fnr)
    }

    /* Mock alle andre tilgangssjekker som true, for å provosere fram lekkasje. */
    private fun mockTilganger(underenhet: Boolean, hovedenhet: Boolean) {
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
