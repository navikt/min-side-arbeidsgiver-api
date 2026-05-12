package no.nav.arbeidsgiver.min_side

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AccessPackageMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadataResponse
import no.nav.arbeidsgiver.min_side.services.altinn.RolleMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.flatten

/** Mock that handles POST /altinn-tilganger with [handler]. GET /resource-metadata returns empty by default. */
fun ExternalServicesBuilder.mockAltinnTilganger(
    handler: suspend RoutingContext.() -> Unit,
) {
    hosts(AltinnTilgangerService.ingress) {
        install(ContentNegotiation) { json() }
        routing {
            post("altinn-tilganger") { handler() }
            get("resource-metadata") { call.respond(RessursMetadataResponse(emptyMap())) }
        }
    }
}

/** Mock that serves [tilgangerResponse] from POST /altinn-tilganger and [ressursMetadataResponse] from GET /resource-metadata. */
fun ExternalServicesBuilder.mockAltinnTilganger(
    tilgangerResponse: AltinnTilganger,
    ressursMetadataResponse: Map<String, RessursMetadata> = emptyMap(),
    accessPackagesMetadataResponse: Map<String, AccessPackageMetadata> = emptyMap(),
    rolesMetadataResponse: Map<String, RolleMetadata> = emptyMap(),
) {
    hosts(AltinnTilgangerService.ingress) {
        install(ContentNegotiation) { json() }
        routing {
            post("altinn-tilganger") { call.respond(tilgangerResponse) }
            get("resource-metadata") { call.respond(RessursMetadataResponse(ressursMetadataResponse, accessPackagesMetadataResponse, rolesMetadataResponse)) }
        }
    }
}

/** Mock with full custom control over both handlers. */
fun ExternalServicesBuilder.mockAltinnTilgangerMedMetadataHandler(
    handler: suspend RoutingContext.() -> Unit,
    ressursMetadataHandler: suspend RoutingContext.() -> Unit,
) {
    hosts(AltinnTilgangerService.ingress) {
        install(ContentNegotiation) { json() }
        routing {
            post("altinn-tilganger") { handler() }
            get("resource-metadata") { ressursMetadataHandler() }
        }
    }
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
        tjeneste: String? = null,
        rolle: String? = null,
    ) = AltinnTilganger.AltinnTilgang(
        navn = "$navn-parent",
        orgnr = "$orgnr-parent",
        organisasjonsform = "AS",
        altinn2Tilganger = setOfNotNull(tjeneste),
        altinn3Tilganger = setOfNotNull(ressurs),
        roller = setOfNotNull(rolle),
        tilgangspakker = emptySet(),
        underenheter = listOf(
            AltinnTilganger.AltinnTilgang(
                navn = navn,
                orgnr = orgnr,
                organisasjonsform = "BEDR",
                altinn2Tilganger = setOfNotNull(tjeneste),
                altinn3Tilganger = setOfNotNull(ressurs),
                roller = setOfNotNull(rolle),
                tilgangspakker = emptySet(),
                underenheter = emptyList()
            )
        )
    )

    fun medTilganger(
        orgnr: String,
        navn: String = orgnr,
        ressurs: String? = null,
        tjeneste: String? = null,
        rolle: String? = null,
    ) = medTilganger(
        medTilgang(
            orgnr = orgnr,
            navn = navn,
            ressurs = ressurs,
            tjeneste = tjeneste,
            rolle = rolle,
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
