package no.nav.arbeidsgiver.min_side.services.digisyfo

import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoRepository.Virksomhetsinfo
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Profile("local", "labs")
@Repository
class DigisyfoRepositoryStub : DigisyfoRepository {
    override fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<Virksomhetsinfo> {
        return listOf(Virksomhetsinfo("910825526", 4))
    }

    override fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse) {}
    override fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>) {}
}