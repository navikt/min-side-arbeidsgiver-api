package no.nav.arbeidsgiver.min_side

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.flatten

fun ExternalServicesBuilder.mockAltinnTilganger(
    handler: suspend RoutingContext.() -> Unit
) {
    hosts(AltinnTilgangerService.ingress) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            post("altinn-tilganger") {
                handler()
            }
        }
    }
}

fun ExternalServicesBuilder.mockAltinnTilganger(
    tilgangerResponse: AltinnTilganger
) = mockAltinnTilganger {
    call.respond(tilgangerResponse)
}

object AltinnTilgangerMock {
    val empty = AltinnTilganger(
        isError = false,
        hierarki = emptyList(),
        orgNrTilTilganger = emptyMap(),
        tilgangTilOrgNr = emptyMap(),
    )

    fun medTilgang(
        orgnr: String,
        navn: String = orgnr,
        ressurs: String? = null,
        tjeneste: String? = null
    ) = AltinnTilganger.AltinnTilgang(
        navn = "$navn-parent",
        orgnr = "$orgnr-parent",
        organisasjonsform = "AS",
        altinn2Tilganger = setOfNotNull(tjeneste),
        altinn3Tilganger = setOfNotNull(ressurs),
        underenheter = listOf(
            AltinnTilganger.AltinnTilgang(
                navn = navn,
                orgnr = orgnr,
                organisasjonsform = "BEDR",
                altinn2Tilganger = setOfNotNull(tjeneste),
                altinn3Tilganger = setOfNotNull(ressurs),
                underenheter = emptyList()
            )
        )
    )

    fun medTilganger(
        orgnr: String,
        navn: String = orgnr,
        ressurs: String? = null,
        tjeneste: String? = null
    ) = medTilganger(
        medTilgang(
            orgnr = orgnr,
            navn = navn,
            ressurs = ressurs,
            tjeneste = tjeneste
        )
    )

    fun medTilganger(
        vararg hierarki: AltinnTilganger.AltinnTilgang,
    ): AltinnTilganger {
        val orgNrTilTilganger = hierarki.flatMap {
            flatten(it) { t ->
                t.orgnr to t.altinn2Tilganger + t.altinn3Tilganger
            }
        }.toMap()
        val tilgangTilOrgNr = orgNrTilTilganger
            .entries
            .flatMap { (orgnr, tilganger) -> tilganger.map { tilgang -> tilgang to orgnr } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, orgnumre) -> orgnumre.toSet() }

        return AltinnTilganger(
            isError = false,
            hierarki = listOf(*hierarki),
            orgNrTilTilganger = orgNrTilTilganger,
            tilgangTilOrgNr = tilgangTilOrgNr
        )
    }
}

