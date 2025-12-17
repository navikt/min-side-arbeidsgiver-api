package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.userinfo.UserInfoService

suspend fun Application.configureUserInfoRoutes() {
    val userInfoService = dependencies.resolve<UserInfoService>()
    msaApiRouting {
        get("userInfo/v3") {
            call.respond(
                userInfoService.getUserInfoV3(
                    fnr = innloggetBruker,
                    token = subjectToken
                )
            )
        }
    }
}