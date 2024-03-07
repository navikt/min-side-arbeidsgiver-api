package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import no.nav.arbeidsgiver.min_side.config.GittMiljø
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SykefraværstatistikkController(
    private val altinnService: AltinnService,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    private val sykefraværstatistikkRepository: SykefraværstatistikkRepository,
    gittMiljø: GittMiljø,
) {

    val tjenestekode = "3403"
    val tjenesteversjon = gittMiljø.resolve(
        prod = { "2" },
        other = { "1" },
    )

    // TODO: gjør v2 om til default når ajour
    @GetMapping("/api/sykefravaerstatistikk/{orgnr}")
    fun getStatistikk(
        @PathVariable orgnr: String,
    ): ResponseEntity<StatistikkRespons> {
        val org = altinnService
            .hentOrganisasjonerBasertPaRettigheter(authenticatedUserHolder.fnr, tjenestekode, tjenesteversjon)
            .firstOrNull { it.organizationNumber == orgnr }

        return if (org != null) {
            val statistikk = sykefraværstatistikkRepository.virksomhetstatistikk_v1(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = org.name ?: it.kode,
                    prosent = it.prosent,
                )
            } ?: sykefraværstatistikkRepository.statistikk_v1(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            }
            statistikk.asResponseEntity()
        } else {
            sykefraværstatistikkRepository.statistikk_v1(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            }.asResponseEntity()
        }
    }

    @GetMapping("/api/sykefravaerstatistikk_v2/{orgnr}")
    fun getStatistikkV2(
        @PathVariable orgnr: String,
    ): ResponseEntity<StatistikkRespons> {
        val org = altinnService
            .hentOrganisasjonerBasertPaRettigheter(authenticatedUserHolder.fnr, tjenestekode, tjenesteversjon)
            .firstOrNull { it.organizationNumber == orgnr }

        return if (org != null) {
            val statistikk = sykefraværstatistikkRepository.virksomhetstatistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = org.name ?: it.kode,
                    prosent = it.prosent,
                )
            } ?: sykefraværstatistikkRepository.statistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            }
            statistikk.asResponseEntity()
        } else {
            sykefraværstatistikkRepository.statistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            }.asResponseEntity()
        }
    }

    // DTO som matcher tidligere respons fra sykefraværsstatistikk-apiet
    data class StatistikkRespons(
        val type: String,
        val label: String,
        val prosent: Double,
    )
}

private fun SykefraværstatistikkController.StatistikkRespons?.asResponseEntity(): ResponseEntity<SykefraværstatistikkController.StatistikkRespons> = if (this == null) {
    ResponseEntity.noContent().build()
} else {
    ResponseEntity.ok().body(this)
}