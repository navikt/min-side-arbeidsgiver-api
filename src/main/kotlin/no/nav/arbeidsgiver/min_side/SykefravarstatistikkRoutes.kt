package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.SykefraværstatistikkService

suspend fun Application.configureSykefravarstatistikkRoutes() {
    val sykefraværstatistikkService = dependencies.resolve<SykefraværstatistikkService>()

    msaApiRouting {
        get("sykefravaerstatistikk/{orgnr}") {
            when (val orgnr = call.parameters["orgnr"]) {
                null -> {
                    call.respond(status = io.ktor.http.HttpStatusCode.BadRequest, "")
                }

                else -> {
                    val result = sykefraværstatistikkService.getStatistikk(orgnr, subjectToken)
                    call.respond(
                        status = result.status,
                        result.body ?: ""
                    )
                }
            }
        }
    }
}