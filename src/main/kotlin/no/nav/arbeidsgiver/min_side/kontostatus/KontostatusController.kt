package no.nav.arbeidsgiver.min_side.kontostatus

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class KontostatusController(
    val kontoregisterClient: KontoregisterClient,
) {
    @PostMapping("/api/kontonummerStatus/v1")
    fun get(
        @RequestBody body: Request,
    ) = when (kontoregisterClient.hentKontonummer(body.virksomhetsnummer)) {
        null -> Response(KontonummerStatus.MANGLER_KONTONUMMER)
        else -> Response(KontonummerStatus.OK)
    }

    data class Request(val virksomhetsnummer: String)

    data class Response(val status: KontonummerStatus)

    enum class KontonummerStatus {
        OK,
        MANGLER_KONTONUMMER,
    }
}
