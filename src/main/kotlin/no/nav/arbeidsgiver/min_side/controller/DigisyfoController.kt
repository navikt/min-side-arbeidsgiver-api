package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykmeldingRepository
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

@ProtectedWithClaims(
    issuer = AuthenticatedUserHolder.TOKENX,
    claimMap = [AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL]
)
@RestController
class DigisyfoController(
    private val digisyfoService: DigisyfoService,
    private val nærmestelederRepository: NærmestelederRepository,
    private val sykmeldingRepository: SykmeldingRepository,
    private val eregService: EregService,
    private val authenticatedUserHolder: AuthenticatedUserHolder
) {

    data class VirksomhetOgAntallSykmeldte(
        val organisasjon: Organisasjon? = null,
        val antallSykmeldte: Int = 0,
    )

    @GetMapping("/api/narmesteleder/virksomheter-v3")
    fun hentVirksomheter(): Collection<VirksomhetOgAntallSykmeldte> {
        val fnr = authenticatedUserHolder.fnr
        return digisyfoService.hentVirksomheterOgSykmeldte(fnr)
    }

    data class DigisyfoOrganisasjonBakoverkompatibel(
        val organisasjon: Organisasjon? = null,
        val antallSykmeldinger: Int = 0,
    )

    @GetMapping("/api/narmesteleder/virksomheter-v2")
    fun hentVirksomheterBakoverkompatibel(): List<DigisyfoOrganisasjonBakoverkompatibel> {
        val fnr = authenticatedUserHolder.fnr
        val aktiveSykmeldingerOversikt = sykmeldingRepository.oversiktSykmeldinger(fnr)
        val harAktiveSykmeldinger = Predicate { virksomhetsnummer: String? ->
            aktiveSykmeldingerOversikt.getOrDefault(
                virksomhetsnummer,
                0
            ) > 0
        }
        return nærmestelederRepository.virksomheterSomNærmesteLeder(fnr)
            .stream()
            .filter(harAktiveSykmeldinger)
            .flatMap { virksomhetsnummer: String? -> hentUnderenhetOgOverenhet(virksomhetsnummer) }
            .filter { obj: Organisasjon? -> Objects.nonNull(obj) }
            .map { org: Organisasjon? ->
                DigisyfoOrganisasjonBakoverkompatibel(
                    org,
                    aktiveSykmeldingerOversikt.getOrDefault(org!!.organizationNumber, 0)
                )
            }
            .collect(Collectors.toList())
    }

    fun hentUnderenhetOgOverenhet(virksomhetsnummer: String?): Stream<Organisasjon?> {
        val underenhet = eregService.hentUnderenhet(virksomhetsnummer)
        var overenhet: Organisasjon? = null
        if (underenhet != null) {
            overenhet = eregService.hentOverenhet(underenhet.parentOrganizationNumber)
        }
        return Stream.of(underenhet, overenhet)
    }
}