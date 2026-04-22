package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService

suspend fun Application.configureAltinnTilgangerRoutes() {
    val altinnTilgangerService = dependencies.resolve<AltinnTilgangerService>()

    msaApiRouting {
        post("altinn-tilganger") {
            call.respond(
                altinnTilgangerService.hentAltinnTilganger(subjectToken)
            )
        }
    }
}
