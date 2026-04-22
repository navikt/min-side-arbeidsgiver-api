package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.rolleVisningsnavn

suspend fun Application.configureAltinnTilgangerRoutes() {
    val altinnTilgangerService = dependencies.resolve<AltinnTilgangerService>()

    msaApiRouting {
        post("altinn-tilganger") {
            call.respond(
                AltinnTilgangerResponse.from(
                    altinnTilgangerService.hentAltinnTilganger(subjectToken)
                )
            )
        }
    }
}

@Serializable
data class AltinnTilgangerResponse(
    val isError: Boolean,
    val hierarki: List<AltinnTilgangResponse>,
    val orgNrTilTilganger: Map<String, Set<String>>,
    val tilgangTilOrgNr: Map<String, Set<String>>,
) {
    companion object {
        fun from(altinnTilganger: AltinnTilganger): AltinnTilgangerResponse = AltinnTilgangerResponse(
            isError = altinnTilganger.isError,
            hierarki = altinnTilganger.hierarki.map(AltinnTilgangResponse::from),
            orgNrTilTilganger = altinnTilganger.orgNrTilTilganger,
            tilgangTilOrgNr = altinnTilganger.tilgangTilOrgNr,
        )
    }
}

@Serializable
data class AltinnTilgangResponse(
    val orgnr: String,
    val navn: String,
    val organisasjonsform: String,
    val altinn3Tilganger: Set<String>,
    val altinn2Tilganger: Set<String>,
    val roller: Set<AltinnRolleResponse>,
    val underenheter: List<AltinnTilgangResponse>,
) {
    companion object {
        fun from(altinnTilgang: AltinnTilganger.AltinnTilgang): AltinnTilgangResponse = AltinnTilgangResponse(
            orgnr = altinnTilgang.orgnr,
            navn = altinnTilgang.navn,
            organisasjonsform = altinnTilgang.organisasjonsform,
            altinn3Tilganger = altinnTilgang.altinn3Tilganger,
            altinn2Tilganger = altinnTilgang.altinn2Tilganger,
            roller = altinnTilgang.roller.map { rolle ->
                AltinnRolleResponse(
                    kode = rolle,
                    visningsnavn = rolleVisningsnavn[rolle] ?: rolle,
                )
            }.toSet(),
            underenheter = altinnTilgang.underenheter.map { underenhet -> from(underenhet) },
        )
    }
}

@Serializable
data class AltinnRolleResponse(
    val kode: String,
    val visningsnavn: String,
)
