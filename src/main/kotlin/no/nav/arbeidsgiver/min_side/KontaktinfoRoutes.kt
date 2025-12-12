package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService

suspend fun Application.configureKontaktinfoRoutes() {
    val kontaktInfoService = dependencies.resolve<KontaktInfoService>()

    msaApiRouting {
        post("kontaktinfo/v1") {
            call.respond(
                kontaktInfoService.getKontaktinfo(call.receive(), innloggetBruker)
            )
        }
    }
}