package no.nav.arbeidsgiver.min_side.services.digisyfo

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig

class DigisyfoService(
    private val digisyfoRepository: DigisyfoRepository,
    private val eregClient: EregClient,
    private val meterRegistry: MeterRegistry
) {

    suspend fun hentVirksomheterOgSykmeldte(authenticatedUserHolder: AuthenticatedUserHolder): List<VirksomhetOgAntallSykmeldte> {
        val virksomheterOgSykmeldte = digisyfoRepository.virksomheterOgSykmeldte(authenticatedUserHolder.fnr)

        val orgs = mutableMapOf<String, VirksomhetOgAntallSykmeldte>()

        for (virksomhetOgSykmeldt in virksomheterOgSykmeldte) {
            val virksomhet = orgs[virksomhetOgSykmeldt.virksomhetsnummer].let {
                if (it == null) {
                    val eregOrg = eregClient.hentUnderenhet(virksomhetOgSykmeldt.virksomhetsnummer)
                        ?: throw RuntimeException("Fant ikke underenhet for $virksomhetOgSykmeldt.virksomhetsnummer")
                    orgs[virksomhetOgSykmeldt.virksomhetsnummer] =
                        VirksomhetOgAntallSykmeldte.from(eregOrg, virksomhetOgSykmeldt.antallSykmeldte)
                    orgs[virksomhetOgSykmeldt.virksomhetsnummer]
                }
                it!!
            }
            berikOverenheter(orgs, virksomhet)
        }

        val underenheter = orgs.values.filter { it.underenheter.isEmpty() }
        val overenheter = orgs.values.filter { it.orgnrOverenhet == null }

        meterRegistry.counter(
            "msa.digisyfo.tilgang",
            "virksomheter",
            underenheter.size.toString()
        ).increment()

        return overenheter
    }

    private suspend fun berikOverenheter(
        orgs: MutableMap<String, VirksomhetOgAntallSykmeldte>,
        virksomhet: VirksomhetOgAntallSykmeldte
    ) {
        val orgnummerTilOverenhet = virksomhet.orgnrOverenhet ?: return

        val overenhet = orgs[orgnummerTilOverenhet].let {
            if (it == null) {
                val eregOrg = eregClient.hentUnderenhet(orgnummerTilOverenhet)
                    ?: throw RuntimeException("Fant ikke underenhet for $orgnummerTilOverenhet")
                orgs[orgnummerTilOverenhet] = VirksomhetOgAntallSykmeldte.from(eregOrg)
                orgs[orgnummerTilOverenhet]
            }
            it!!
        }

        overenhet.leggTilUnderenhet(virksomhet)
        berikOverenheter(orgs, overenhet)
    }

    data class VirksomhetOgAntallSykmeldte(
        val orgnr: String,
        val navn: String,
        val organisasjonsform: String,
        var antallSykmeldte: Int,
        val underenheter: MutableList<VirksomhetOgAntallSykmeldte>,
        @JsonIgnore val orgnrOverenhet: String?,
    ) {
        fun leggTilUnderenhet(underenhet: VirksomhetOgAntallSykmeldte) {
            val eksisterende = underenheter.firstOrNull { it.orgnr == underenhet.orgnr }
            if (eksisterende !== null) {
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
            fun from(eregOrganisasjon: EregOrganisasjon, antallSykmeldte: Int = 0) =
                VirksomhetOgAntallSykmeldte(
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