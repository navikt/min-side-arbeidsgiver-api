package no.nav.arbeidsgiver.min_side

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangSoknadService

suspend fun Application.configureTilgangssoknadRoutes() {
    val altinnTilgangSoknadService = dependencies.resolve<AltinnTilgangSoknadService>()

    msaApiRouting {
        post("delegation-request") {
            val response = altinnTilgangSoknadService.opprettDelegationRequest(
                fnr = innloggetBruker,
                token = subjectToken,
                request = call.receive(),
            )
            if (response == null) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(HttpStatusCode.Accepted, response)
            }
        }

        get("delegation-request") {
            call.respond(
                altinnTilgangSoknadService.mineDelegationRequests(innloggetBruker)
            )
        }
    }
}
