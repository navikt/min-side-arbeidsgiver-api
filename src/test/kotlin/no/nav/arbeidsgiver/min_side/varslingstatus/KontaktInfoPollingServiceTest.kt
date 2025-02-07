package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.kontaktinfo.KontaktinfoClient.Kontaktinfo
import no.nav.arbeidsgiver.min_side.services.ereg.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [
        KontaktInfoPollingService::class
    ],
    properties = [
        "spring.flyway.cleanDisabled=false",
    ]
)
@MockitoBean(
    types = [
        VarslingStatusRepository::class,
        KontaktinfoClient::class,
        EregService::class,
        KontaktInfoPollerRepository::class,
    ]
)
class KontaktInfoPollingServiceTest {
    @Autowired
    lateinit var kontaktInfoPollingService: KontaktInfoPollingService

    @Autowired
    lateinit var kontaktinfoPollerRepository: KontaktInfoPollerRepository

    @Autowired
    lateinit var kontaktinfoClient: KontaktinfoClient

    @Autowired
    lateinit var eregService: EregService


    @Test
    fun `bruker kofuvi for underenhet om den finnes`() {
        // given
        `when`(kontaktinfoPollerRepository.getAndDeleteForPoll()).thenReturn(underenhetOrgnr)
        `when`(eregService.hentUnderenhet(underenhetOrgnr)).thenReturn(underenhet)
        `when`(kontaktinfoClient.hentKontaktinfo(underenhetOrgnr)).thenReturn(kontaktinfoMedEpost)
        `when`(kontaktinfoClient.hentKontaktinfo(hovedenhetOrgnr)).thenReturn(kontaktinfoMedTlf)

        // when
        kontaktInfoPollingService.pollAndPullKontaktInfo()

        // then
        verify(kontaktinfoPollerRepository).updateKontaktInfo(
            underenhetOrgnr,
            harEpost = true,
            harTlf = false,
        )
    }

    @Test
    fun `henter kofuvi for hovedenhet om det mangler kofuvi på underenhet`() {
        // given
        `when`(kontaktinfoPollerRepository.getAndDeleteForPoll()).thenReturn(underenhetOrgnr)
        `when`(eregService.hentUnderenhet(underenhetOrgnr)).thenReturn(underenhet)
        `when`(kontaktinfoClient.hentKontaktinfo(underenhetOrgnr)).thenReturn(ingenKontaktinfo)
        `when`(kontaktinfoClient.hentKontaktinfo(hovedenhetOrgnr)).thenReturn(kontaktinfoMedEpost)

        // when
        kontaktInfoPollingService.pollAndPullKontaktInfo()

        // then
        verify(kontaktinfoPollerRepository).updateKontaktInfo(
            underenhetOrgnr,
            harEpost = true,
            harTlf = false,
        )
    }

    companion object {
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
}