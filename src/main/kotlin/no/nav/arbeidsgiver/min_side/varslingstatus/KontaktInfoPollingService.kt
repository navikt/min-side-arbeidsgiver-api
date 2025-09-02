package no.nav.arbeidsgiver.min_side.varslingstatus

import kotlinx.coroutines.delay
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class KontaktInfoPollingService(
    private val varslingStatusRepository: VarslingStatusRepository,
    private val kontaktinfoClient: KontaktinfoClient,
    private val eregClient: EregClient,
    private val kontaktInfoPollerRepository: KontaktInfoPollerRepository,
) {

    val retention = 90.days

    suspend fun schedulePolling() {
        val virksomheterMedFeil = varslingStatusRepository.hentVirksomheterMedFeil(retention)
        kontaktInfoPollerRepository.schedulePoll(
            virksomheterMedFeil,
            Instant.now().toString()
        )
        delay(Duration.parse("PT60M"))
    }



    //TODO: @Transactional
    suspend fun pollAndPullKontaktInfo() {
        val virksomhetsnummer = kontaktInfoPollerRepository.getAndDeleteForPoll() ?: return
        val kontaktInfo = finnKontaktinfoIOrgTre(virksomhetsnummer) ?: return

        kontaktInfoPollerRepository.updateKontaktInfo(
            virksomhetsnummer,
            kontaktInfo.eposter.isNotEmpty(),
            kontaktInfo.telefonnumre.isNotEmpty()
        )
        delay(Duration.parse("PT1S"))
    }


    //TODO: @Transactional
    suspend fun cleanup() {
        varslingStatusRepository.slettVarslingStatuserEldreEnn(retention)
        kontaktInfoPollerRepository.slettKontaktinfoMedOkStatusEllerEldreEnn(retention)
        delay(Duration.parse("PT1H"))
    }

    private suspend fun finnKontaktinfoIOrgTre(virksomhetsnummer: String): KontaktinfoClient.Kontaktinfo? {
        val kontaktinfoUnderenhet = kontaktinfoClient.hentKontaktinfo(virksomhetsnummer)
        if (kontaktinfoUnderenhet.harKontaktinfo) {
            return kontaktinfoUnderenhet
        }

        return eregClient.hentUnderenhet(virksomhetsnummer)?.orgnummerTilOverenhet()
            ?.let { kontaktinfoClient.hentKontaktinfo(it) }
    }

}