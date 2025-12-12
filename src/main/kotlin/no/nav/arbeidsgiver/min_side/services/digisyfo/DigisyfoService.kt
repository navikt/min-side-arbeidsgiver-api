package no.nav.arbeidsgiver.min_side.services.digisyfo

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService.VirksomhetOgAntallSykmeldte
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig

interface DigisyfoService {
    suspend fun hentVirksomheterOgSykmeldte(fnr: String): List<VirksomhetOgAntallSykmeldte>

    @Serializable
    data class VirksomhetOgAntallSykmeldte(
        val orgnr: String,
        val navn: String,
        val organisasjonsform: String,
        var antallSykmeldte: Int,
        val underenheter: MutableList<VirksomhetOgAntallSykmeldte>,
        @Transient val orgnrOverenhet: String? = null,
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

class DigisyfoServiceImpl(
    private val digisyfoRepository: DigisyfoRepository,
    private val eregClient: EregClient,
) : DigisyfoService {

    override suspend fun hentVirksomheterOgSykmeldte(fnr: String): List<VirksomhetOgAntallSykmeldte> {
        val virksomheterOgSykmeldte = digisyfoRepository.virksomheterOgSykmeldte(fnr)

        val orgs = mutableMapOf<String, VirksomhetOgAntallSykmeldte>()

        for (virksomhetOgSykmeldt in virksomheterOgSykmeldte) {
            val virksomhet = orgs[virksomhetOgSykmeldt.virksomhetsnummer].let {
                if (it == null) {
                    val eregOrg = eregClient.hentOrganisasjon(virksomhetOgSykmeldt.virksomhetsnummer)
                        ?: throw RuntimeException("Fant ikke organisasjon for ${virksomhetOgSykmeldt.virksomhetsnummer}")
                    orgs[virksomhetOgSykmeldt.virksomhetsnummer] =
                        VirksomhetOgAntallSykmeldte.from(eregOrg, virksomhetOgSykmeldt.antallSykmeldte)
                    orgs[virksomhetOgSykmeldt.virksomhetsnummer]!!
                }
                else{
                    it
                }
            }
            berikOverenheter(orgs, virksomhet)
        }

        val overenheter = orgs.values.filter { it.orgnrOverenhet == null }
        return overenheter
    }

    private suspend fun berikOverenheter(
        orgs: MutableMap<String, VirksomhetOgAntallSykmeldte>,
        virksomhet: VirksomhetOgAntallSykmeldte
    ) {
        val orgnummerTilOverenhet = virksomhet.orgnrOverenhet ?: return

        val overenhet = orgs[orgnummerTilOverenhet].let {
            if (it == null) {
                val eregOrg = eregClient.hentOrganisasjon(orgnummerTilOverenhet)
                    ?: throw RuntimeException("Fant ikke underenhet for $orgnummerTilOverenhet")
                orgs[orgnummerTilOverenhet] = VirksomhetOgAntallSykmeldte.from(eregOrg)
                orgs[orgnummerTilOverenhet]!!
            }
            else{
                it
            }
        }

        overenhet.leggTilUnderenhet(virksomhet)
        berikOverenheter(orgs, overenhet)
    }
}