package no.nav.arbeidsgiver.min_side

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontostatusService

suspend fun Application.configureKontonummerRoutes() {
    val kontostatusService = dependencies.resolve<KontostatusService>()

    msaApiRouting {
        post("kontonummerStatus/v1") {
            call.respond(
                kontostatusService.getKontonummerStatus(call.receive())
            )
        }
        post("kontonummer/v1") {
            call.respond(
                kontostatusService.getKontonummer(
                    call.receive(),
                    subjectToken
                ) ?: HttpStatusCode.NotFound
            )
        }
    }
}