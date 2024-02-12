package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoRepository.Virksomhetsinfo
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import org.springframework.stereotype.Component

@Component
class DigisyfoService(
    private val digisyfoRepository: DigisyfoRepository,
    private val eregService: EregService,
    private val meterRegistry: MeterRegistry
) {

    data class VirksomhetOgAntallSykmeldte(
        val organisasjon: Organisasjon,
        val antallSykmeldte: Int,
    )

    fun hentVirksomheterOgSykmeldte(fnr: String): Collection<VirksomhetOgAntallSykmeldte> {
        val underenheter = digisyfoRepository.virksomheterOgSykmeldte(fnr)
            .flatMap { hentUnderenhet(it) }
        val overenheterOrgnr = underenheter
            .mapNotNull { it.organisasjon.parentOrganizationNumber }
            .toSet()
        val resultat = underenheter + overenheterOrgnr
            .flatMap { hentOverenhet(it) }

        meterRegistry.counter(
            "msa.digisyfo.tilgang",
            "virksomheter",
            resultat.size.toString()
        ).increment()

        return resultat.distinctBy { it.organisasjon.organizationNumber }
    }

    private fun hentForfedre(org: Organisasjon, orgs: MutableList<Organisasjon> = mutableListOf()): List<Organisasjon> {
        if (org.parentOrganizationNumber == null) return orgs

        val overenhet = eregService.hentOverenhet(org.parentOrganizationNumber!!) ?: return orgs

        orgs.add(overenhet)
        return hentForfedre(overenhet, orgs)

    }


    private fun hentOverenhet(orgnr: String): List<VirksomhetOgAntallSykmeldte> {
        val hovedenhet = eregService.hentOverenhet(orgnr) ?: return listOf()
        val forfedre = hentForfedre(hovedenhet)
        val result = mutableListOf<VirksomhetOgAntallSykmeldte>()
        result.add(VirksomhetOgAntallSykmeldte(hovedenhet, 0))
        result.addAll(forfedre.map { VirksomhetOgAntallSykmeldte(it, 0) })
        return result
    }

    private fun hentUnderenhet(virksomhetsinfo: Virksomhetsinfo): List<VirksomhetOgAntallSykmeldte> {
        val underenhet = eregService.hentUnderenhet(virksomhetsinfo.virksomhetsnummer)
            ?: return listOf()
        return listOf(
            VirksomhetOgAntallSykmeldte(
                underenhet,
                virksomhetsinfo.antallSykmeldte
            )
        )
    }
}