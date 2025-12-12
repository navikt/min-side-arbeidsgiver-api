package no.nav.arbeidsgiver.min_side.kontaktinfo

import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.services.ereg.*
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService.KontaktinfoRequest
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient.Kontaktinfo
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class KontaktinfoServiceTest {

    private val subjectFnr = "012345678"
    private val orgnrUnderenhet = "0".repeat(9)
    private val orgnrHovedenhet = "1".repeat(9)
    private val orgnrAnnet = "2".repeat(9)


    /**
     * også kall med andre orgnr er vellykkede, for å unngå early return i controlleren,
     * som kunne ha skjult manglende tilgangssjekker.
     */
    private val mockEregClient = object : EregClient {
        override suspend fun hentOrganisasjon(orgnummer: String) =
            if (orgnummer == orgnrUnderenhet) {
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

    /**
     * Returner alltid kontaktinfo, uavhengig av orgnr, så vi ikke skjuler feil.
     */
    private val mockKontaktInfoClient = object : KontaktinfoClient {
        override suspend fun hentKontaktinfo(orgnr: String) =
            Kontaktinfo(
                eposter = setOf("a@$orgnr.test"),
                telefonnumre = setOf("+47$orgnr")
            )
    }

    @Test
    fun `tilgang til både underenhet og hovedenhet`() = runTestApplication {
        KontaktInfoService(
            eregClient = mockEregClient,
            kontaktinfoClient = mockKontaktInfoClient,
            altinnRollerClient = mockAltinnRollerClient(underenhet = true, hovedenhet = true)
        ).getKontaktinfo(
            KontaktinfoRequest(orgnrUnderenhet),
            subjectFnr
        ).let { kontakinfo ->
            assertNotNull(kontakinfo.hovedenhet)
            assertNotNull(kontakinfo.underenhet)

        }
    }

    @Test
    fun `tilgang til kun underenhet`() = runTestApplication {
        KontaktInfoService(
            eregClient = mockEregClient,
            kontaktinfoClient = mockKontaktInfoClient,
            altinnRollerClient = mockAltinnRollerClient(underenhet = true, hovedenhet = false)
        ).getKontaktinfo(
            KontaktinfoRequest(orgnrUnderenhet),
            subjectFnr
        ).let { kontakinfo ->
            assertNull(kontakinfo.hovedenhet)
            assertNotNull(kontakinfo.underenhet)
        }
    }

    @Test
    fun `tilgang til kun hovedenhet`() = runTestApplication {
        KontaktInfoService(
            eregClient = mockEregClient,
            kontaktinfoClient = mockKontaktInfoClient,
            altinnRollerClient = mockAltinnRollerClient(underenhet = false, hovedenhet = true)
        ).getKontaktinfo(
            KontaktinfoRequest(orgnrUnderenhet),
            subjectFnr
        ).let { kontakinfo ->
            assertNotNull(kontakinfo.hovedenhet)
            assertNull(kontakinfo.underenhet)
        }
    }

    @Test
    fun `ikke tilgang til hverken hovedenhet eller underenhet `() = runTestApplication {
        KontaktInfoService(
            eregClient = mockEregClient,
            kontaktinfoClient = mockKontaktInfoClient,
            altinnRollerClient = mockAltinnRollerClient(underenhet = false, hovedenhet = false)
        ).getKontaktinfo(
            KontaktinfoRequest(orgnrUnderenhet),
            subjectFnr
        ).let { kontakinfo ->
            assertNull(kontakinfo.hovedenhet)
            assertNull(kontakinfo.underenhet)
        }
    }

    /* Mock alle andre tilgangssjekker som true, for å provosere fram lekkasje. */
    private fun mockAltinnRollerClient(underenhet: Boolean, hovedenhet: Boolean): AltinnRollerClient {
        return object : AltinnRollerClient {
            override suspend fun harAltinnRolle(
                fnr: String,
                orgnr: String,
                altinnRoller: Set<String>,
                externalRoller: Set<String>
            ) = when (fnr) {
                subjectFnr -> when (orgnr) {
                    orgnrUnderenhet -> underenhet
                    orgnrHovedenhet -> hovedenhet
                    else -> true
                }

                else -> true
            }
        }
    }
}