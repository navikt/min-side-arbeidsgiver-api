package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksRepository.Pushboks
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
class PushboksController(
    val authenticatedUserHolder: AuthenticatedUserHolder,
    val altinnService: AltinnService,
    val pushboksService: PushboksService,
) {

    //@ProtectedWithClaims(
    //    issuer = AuthenticatedUserHolder.TOKENX,
    //    claimMap = [AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL]
    //)
    @Unprotected
    @GetMapping(path = [
        "/api/pushboks",
        "/api/pushboks/",
    ])
    fun hentPushbokser(
        @RequestParam virksomhetsnummer: String,
    ): List<Pushboks> {
        return pushboksService.hent(virksomhetsnummer)
        // TODO: tilgangsstyring?
        //val virksomhetsnummre = altinnService.hentOrganisasjonerBasertPaRettigheter(authenticatedUserHolder.fnr).map { it.organizationNumber }
        //if (!virksomhetsnummre.contains(virksomhetsnummer)) {
        //    // TODO: throw as forbidden?
        //    return listOf()
        //}
    }


    /**
     * TODO: offbokse denne til egen app slik at last mot api ikke påvirker min side
     * TODO: protect w tokenx azp
     */
    @Unprotected
    @PutMapping("/api/pushboks/{tjeneste}/{virksomhetsnummer}")
    @ResponseStatus(HttpStatus.OK)
    fun upsertBoks(
        @PathVariable tjeneste: String,
        @PathVariable virksomhetsnummer: String,
        @RequestBody innhold: String?,
    ): String {
        pushboksService.upsert(tjeneste, virksomhetsnummer, innhold)
        return "ok"
    }


}