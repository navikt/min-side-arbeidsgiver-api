package no.nav.arbeidsgiver.min_side.services.digisyfo

interface DigisyfoRepository {
    data class Virksomhetsinfo(
        val virksomhetsnummer: String = "",
        val antallSykmeldte: Int = 0
    )

    fun virksomheterOgSykmeldte(nærmestelederFnr: String): List<Virksomhetsinfo>
    fun processNærmesteLederEvent(hendelse: NarmesteLederHendelse)
    fun processSykmeldingEvent(records: List<Pair<String?, SykmeldingHendelse?>>)
}