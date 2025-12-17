package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangSoknadService

suspend fun Application.configureTilgangssoknadRoutes() {
    val altinnTilgangSoknadService = dependencies.resolve<AltinnTilgangSoknadService>()

    msaApiRouting {
        get("altinn-tilgangssoknad") {
            call.respond(
                altinnTilgangSoknadService.mineSøknaderOmTilgang(innloggetBruker)
            )
        }
        post("altinn-tilgangssoknad") {
            val result = altinnTilgangSoknadService.sendSøknadOmTilgang(
                call.receive(),
                fnr = innloggetBruker,
                token = subjectToken
            )


            call.respond(
                status = result.status,
                result.body ?: ""
            )
        }
    }
}