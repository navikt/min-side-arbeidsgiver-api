package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusService

suspend fun Application.configureVarslingStatusRoutes() {
    val varslingStatusService = dependencies.resolve<VarslingStatusService>()

    msaApiRouting {
        post("varslingStatus/v1") {
            call.respond(
                varslingStatusService.getVarslingStatus(
                    call.receive(),
                    subjectToken
                )
            )
        }
    }
}