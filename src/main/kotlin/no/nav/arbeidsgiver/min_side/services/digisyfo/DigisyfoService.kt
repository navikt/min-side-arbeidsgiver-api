package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig
import org.springframework.stereotype.Component

@Component
class DigisyfoService(
    private val digisyfoRepository: DigisyfoRepository,
    private val eregService: EregService,
    private val meterRegistry: MeterRegistry
) {

    fun hentVirksomheterOgSykmeldteV3(fnr: String): List<VirksomhetOgAntallSykmeldteV3> {
        val virksomheterOgSykmeldte = digisyfoRepository.virksomheterOgSykmeldte(fnr)

        val orgs = mutableMapOf<String, VirksomhetOgAntallSykmeldteV3>()

        for (virksomhetOgSykmeldt in virksomheterOgSykmeldte) {
            val virksomhet = orgs.computeIfAbsent(virksomhetOgSykmeldt.virksomhetsnummer) {
                val eregOrg = eregService.hentUnderenhet(it) ?: throw RuntimeException("Fant ikke underenhet for $it")
                VirksomhetOgAntallSykmeldteV3.from(eregOrg, virksomhetOgSykmeldt.antallSykmeldte)
            }
            orgs[virksomhet.orgnr] = virksomhet
            berikOverenheter(orgs, virksomhet)
        }

        val underenheter = orgs.values.filter {it.underenheter.isEmpty() }
        val overenheter = orgs.values.filter {it.orgnrOverenhet == null }

        meterRegistry.counter(
            "msa.digisyfo.tilgang",
            "virksomheter",
            underenheter.size.toString()
        ).increment()

        return overenheter
    }

    private fun berikOverenheter(orgs: MutableMap<String, VirksomhetOgAntallSykmeldteV3>, virksomhet: VirksomhetOgAntallSykmeldteV3) {
        val orgnummerTilOverenhet = virksomhet.orgnrOverenhet ?: return

        val overenhet = orgs.computeIfAbsent(orgnummerTilOverenhet) {
            val eregOrg = eregService.hentOverenhet(it) ?: throw RuntimeException("Fant ikke overenhet for $it")
            VirksomhetOgAntallSykmeldteV3.from(eregOrg, 0)
        }
        orgs[overenhet.orgnr] = overenhet
        overenhet.leggTilUnderenhet(virksomhet)
        berikOverenheter(orgs, overenhet)
    }

    data class VirksomhetOgAntallSykmeldteV3(
        val orgnr: String,
        val navn: String,
        val organisasjonsform: String,
        var antallSykmeldte: Int,
        val underenheter: MutableList<VirksomhetOgAntallSykmeldteV3>,
        @JsonIgnore val orgnrOverenhet: String?,
    ) {
        fun leggTilUnderenhet(underenhet: VirksomhetOgAntallSykmeldteV3) {
            val eksisterende = underenheter.firstOrNull { it.orgnr == underenhet.orgnr }
            if (eksisterende !== null) {
                // legg til barnebarn
                underenhet.underenheter.forEach {
                    eksisterende.leggTilUnderenhet(it)
                }
            } else {
                underenheter.add(underenhet)
                underenheter.sortBy { it.orgnr }
            }
            antallSykmeldte = underenheter.sumOf { it.antallSykmeldte }
        }

        companion object {
            fun from(eregOrganisasjon: EregOrganisasjon, antallSykmeldte: Int) =
                VirksomhetOgAntallSykmeldteV3(
                    orgnr = eregOrganisasjon.organisasjonsnummer,
                    navn = eregOrganisasjon.navn.sammensattnavn,
                    organisasjonsform = eregOrganisasjon.organisasjonDetaljer.enhetstyper?.first {
                        it.gyldighetsperiode.erGyldig()
                    }?.enhetstype ?: "",
                    antallSykmeldte = antallSykmeldte,
                    orgnrOverenhet = eregOrganisasjon.orgnummerTilOverenhet(),
                    underenheter = mutableListOf(),
                )
        }
    }
}