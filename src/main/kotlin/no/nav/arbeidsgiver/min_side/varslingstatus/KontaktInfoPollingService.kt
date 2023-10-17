package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.kontaktinfo.KontaktinfoClient
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.time.Duration.Companion.days

@Profile("dev-gcp", "prod-gcp")
@Service
class KontaktInfoPollingService(
    private val varslingStatusRepository: VarslingStatusRepository,
    private val kontaktinfoClient: KontaktinfoClient,
    private val kontaktInfoPollerRepository: KontaktInfoPollerRepository,
) {

    val retention = 90.days

    @Scheduled(
        initialDelayString = "PT1M",
        fixedDelayString = "PT60M",
    )
    fun schedulePolling() {
        val virksomheterMedFeil = varslingStatusRepository.hentVirksomheterMedFeil(retention)
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
    @Transactional
    fun cleanup() {
        varslingStatusRepository.slettVarslingStatuserEldreEnn(retention)
        kontaktInfoPollerRepository.slettKontaktinfoMedOkStatusEllerEldreEnn(retention)
    }

}