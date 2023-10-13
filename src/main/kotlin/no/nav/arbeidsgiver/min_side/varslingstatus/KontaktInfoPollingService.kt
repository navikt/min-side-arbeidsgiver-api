package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.kontaktinfo.KontaktinfoClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class KontaktInfoPollingService(
    private val varslingStatusRepository: VarslingStatusRepository,
    private val kontaktinfoClient: KontaktinfoClient,
    private val kontaktInfoPollerRepository: KontaktInfoPollerRepository,
) {
    @Scheduled(
        initialDelayString = "PT1M",
        fixedDelayString = "PT60M",
    )
    fun schedulePolling() {
        val virksomheterMedFeil = varslingStatusRepository.hentVirksomheterMedFeil()
        kontaktInfoPollerRepository.schedulePoll(
            virksomheterMedFeil,
            Instant.now().toString()
        )
    }


    @Scheduled(
        initialDelayString = "PT1M",
        fixedDelayString = "PT1S",
    )
    @Transactional
    fun pollAndPullKontaktInfo() {
        val virksomhetsnummer = kontaktInfoPollerRepository.getAndDeleteForPoll() ?: return
        val kontaktInfo = kontaktinfoClient.hentKontaktinfo(virksomhetsnummer) ?: return

        kontaktInfoPollerRepository.updateKontaktInfo(
            virksomhetsnummer,
            kontaktInfo.eposter.isNotEmpty(),
            kontaktInfo.telefonnumre.isNotEmpty()
        )
    }

    @Scheduled(
        initialDelayString = "PT1M",
        fixedDelayString = "PT1H",
    )
    fun cleanupOldAndOk() {
        // TODO: cleanup kontaktinfo_resultat where siste varsel_status = OK, or older than 3 months
    }

}