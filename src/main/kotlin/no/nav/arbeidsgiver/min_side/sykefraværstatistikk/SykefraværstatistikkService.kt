package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SykefraværstatistikkService(
    private val altinnService: AltinnService,
    private val sykefraværstatistikkRepository: SykefraværstatistikkRepository,
) {

    val altinnRessursId = "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"

    @GetMapping("/api/sykefravaerstatistikk/{orgnr}")
    fun getStatistikk(
        @PathVariable orgnr: String,
    ): ResponseEntity<StatistikkRespons> {
        val harTilgang = altinnService.harTilgang(orgnr, altinnRessursId)

        return if (harTilgang) {
            val statistikk = sykefraværstatistikkRepository.virksomhetstatistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
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

private fun SykefraværstatistikkService.StatistikkRespons?.asResponseEntity(): ResponseEntity<SykefraværstatistikkService.StatistikkRespons> =
    if (this == null) {
        ResponseEntity.noContent().build()
    } else {
        ResponseEntity.ok().body(this)
    }