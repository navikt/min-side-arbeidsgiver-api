package no.nav.arbeidsgiver.min_side.kontostatus

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

const val kontonummerTilgangTjenesetekode = "2896:87"

@RestController
class KontostatusController(
    val kontoregisterClient: KontoregisterClient,
    val altinnService: AltinnService
) {

    @PostMapping("/api/kontonummerStatus/v1")
    fun getKontonummerStatus(
        @RequestBody body: Request,
    ) = when (kontoregisterClient.hentKontonummer(body.virksomhetsnummer)) {
        null -> Response(KontonummerStatus.MANGLER_KONTONUMMER)
        else -> Response(KontonummerStatus.OK)
    }

    @PostMapping("/api/kontonummer/v1")
    fun getKontoNummer(
        @RequestBody body: Request,
    ): Response? {
        val harTilgang = altinnService.harTilgang(body.virksomhetsnummer, kontonummerTilgangTjenesetekode)
        if (!harTilgang) {
            return null
        }
        return when (val oppslag = kontoregisterClient.hentKontonummer(body.virksomhetsnummer)) {
            null -> Response(KontonummerStatus.MANGLER_KONTONUMMER)
            else -> Response(status = KontonummerStatus.OK, kontonummer = oppslag.kontonr, orgnr = oppslag.mottaker)
        }
    }

    data class Request(val virksomhetsnummer: String)

    data class Response(
        val status: KontonummerStatus,
        val kontonummer: String? = null,
        val orgnr: String? = null
    )

    enum class KontonummerStatus {
        OK,
        MANGLER_KONTONUMMER,
    }
}
