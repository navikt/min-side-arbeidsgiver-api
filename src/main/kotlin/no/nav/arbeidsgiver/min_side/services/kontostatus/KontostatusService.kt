package no.nav.arbeidsgiver.min_side.services.kontostatus

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService

const val kontonummerTilgangTjenesetekode = "2896:87"

class KontostatusService(
    val kontoregisterClient: KontoregisterClient,
    val altinnService: AltinnService
) {

    suspend fun getKontonummerStatus(
        body: StatusRequest,
    ) = when (kontoregisterClient.hentKontonummer(body.virksomhetsnummer)) {
        null -> StatusResponse(KontonummerStatus.MANGLER_KONTONUMMER)
        else -> StatusResponse(KontonummerStatus.OK)
    }

    /**
     * Henter kontonummer for en gitt organisasjon.
     * Kontonummer tilgangstyres på overordnet enhet, ikke på underenhet.
     * Dersom bruker har tilgang på overordnet enhet, har hen også tilgang på underenhet (https://nav-it.slack.com/archives/CKZADNFBP/p1736263494923189)
     */
    suspend fun getKontonummer(
        body: OppslagRequest,
    ): OppslagResponse? {
        val harTilgang = altinnService.harTilgang(body.orgnrForTilgangstyring, kontonummerTilgangTjenesetekode)
        if (!harTilgang) {
            return null
        }
        return when (val oppslag = kontoregisterClient.hentKontonummer(body.orgnrForOppslag)) {
            null -> OppslagResponse(KontonummerStatus.MANGLER_KONTONUMMER)
            else -> OppslagResponse(
                status = KontonummerStatus.OK,
                kontonummer = oppslag.kontonr,
                orgnr = oppslag.mottaker
            )
        }
    }

    data class StatusRequest(val virksomhetsnummer: String)
    data class StatusResponse(val status: KontonummerStatus)


    data class OppslagRequest(val orgnrForTilgangstyring: String, val orgnrForOppslag: String)

    data class OppslagResponse(
        val status: KontonummerStatus,
        val kontonummer: String? = null,
        val orgnr: String? = null
    )

    enum class KontonummerStatus {
        OK,
        MANGLER_KONTONUMMER,
    }
}
