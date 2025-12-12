package no.nav.arbeidsgiver.min_side

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.services.ereg.EregService

suspend fun Application.configureEregRoutes() {
    val eregService = dependencies.resolve<EregService>()

    msaApiRouting {
        post("ereg/underenhet") {
            call.respond(
                eregService.hentOrganisasjon(call.receive()) ?: HttpStatusCode.OK
            )
        }
        post("ereg/overenhet") {
            call.respond(
                eregService.hentOrganisasjon(call.receive()) ?: HttpStatusCode.OK
            )
        }
    }
}