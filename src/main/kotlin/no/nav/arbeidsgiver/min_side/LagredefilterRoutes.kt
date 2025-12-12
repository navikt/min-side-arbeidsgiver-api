package no.nav.arbeidsgiver.min_side

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService

suspend fun Application.configureLagredefilterRoutes() {
    val lagredeFilterService = dependencies.resolve<LagredeFilterService>()

    msaApiRouting {
        get("lagredeFilter") {
            call.respond(
                lagredeFilterService.getAll(fnr = innloggetBruker)
            )
        }
        put("lagredeFilter") {
            call.respond(
                lagredeFilterService.put(call.receive(), fnr = innloggetBruker)
            )
        }
        delete("lagredeFilter/{filterId}") {
            call.respond(
                lagredeFilterService.delete(
                    call.parameters["filterId"]!!,
                    fnr = innloggetBruker
                ) ?: HttpStatusCode.NotFound
            )
        }

    }
}