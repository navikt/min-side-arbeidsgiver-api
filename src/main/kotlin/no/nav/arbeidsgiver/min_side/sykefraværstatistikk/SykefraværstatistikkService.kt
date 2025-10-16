package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import io.ktor.http.HttpStatusCode
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssøknad


class SykefraværstatistikkService(
    private val altinnService: AltinnService,
    private val sykefraværstatistikkRepository: SykefraværstatistikkRepository,
) {

    val altinnRessursId = "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"

    suspend fun getStatistikk(
        orgnr: String,
        token: String
    ): ResponseEntity {
        val harTilgang = altinnService.harTilgang(orgnr, altinnRessursId, token)

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
    data class ResponseEntity(
        val status: HttpStatusCode,
        val body: StatistikkRespons? = null,
    )
}


private fun SykefraværstatistikkService.StatistikkRespons?.asResponseEntity(): SykefraværstatistikkService.ResponseEntity =
    if (this == null) {
        SykefraværstatistikkService.ResponseEntity(HttpStatusCode.NoContent)
    } else {
        SykefraværstatistikkService.ResponseEntity(HttpStatusCode.OK, this)
    }