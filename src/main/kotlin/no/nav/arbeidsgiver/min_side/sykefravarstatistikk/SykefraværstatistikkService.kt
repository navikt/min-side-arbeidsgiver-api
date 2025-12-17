package no.nav.arbeidsgiver.min_side.sykefravarstatistikk

import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService


class SykefraværstatistikkService(
    private val altinnTilgangerService: AltinnTilgangerService,
    private val sykefravarstatistikkRepository: SykefravarstatistikkRepository,
) {

    val altinnRessursId = "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"

    suspend fun getStatistikk(
        orgnr: String,
        token: String
    ): ResponseEntity {
        val harTilgang = altinnTilgangerService.harTilgang(orgnr, altinnRessursId, token)

        return if (harTilgang) {
            val statistikk = sykefravarstatistikkRepository.virksomhetstatistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            } ?: sykefravarstatistikkRepository.statistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            }
            statistikk.asResponseEntity()
        } else {
            sykefravarstatistikkRepository.statistikk(orgnr)?.let {
                StatistikkRespons(
                    type = it.kategori,
                    label = it.kode,
                    prosent = it.prosent,
                )
            }.asResponseEntity()
        }
    }


    // DTO som matcher tidligere respons fra sykefraværsstatistikk-apiet
    @Serializable
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