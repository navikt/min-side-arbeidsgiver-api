package no.nav.arbeidsgiver.min_side.kontaktinfo

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import org.springframework.web.bind.annotation.*

@RestController
class KontaktinfoController(
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    private val altinnRollerClient: AltinnRollerClient,
    private val eregService: EregService,
    private val kontaktinfoClient: KontaktinfoClient,
) {
    @PostMapping("/api/kontaktinfo/v1")
    fun getKontaktinfo(@RequestBody requestBody: KontaktinfoRequest): KontaktinfoResponse {
        val orgnrUnderenhet = requestBody.virksomhetsnummer
        val orgnrHovedenhet = eregService.hentUnderenhet(orgnrUnderenhet)
            ?.parentOrganizationNumber
            ?: return KontaktinfoResponse(null, null)

        return KontaktinfoResponse(
            underenhet = tilgangsstyrOgHentKontaktinfo(orgnrUnderenhet),
            hovedenhet = tilgangsstyrOgHentKontaktinfo(orgnrHovedenhet),
        )
    }

    private fun tilgangsstyrOgHentKontaktinfo(orgnr: String): Kontaktinfo? {
        val tilgangHovedenhet = altinnRollerClient.harAltinnRolle(
            fnr = authenticatedUserHolder.fnr,
            orgnr = orgnr,
            altinnRoller = ALTINN_ROLLER,
            externalRoller = EXTERNAL_ROLLER,
        )

        return if (tilgangHovedenhet) {
            kontaktinfoClient.hentKontaktinfo(orgnr)?.let {
                Kontaktinfo(
                    eposter = it.eposter.toList(),
                    telefonnumre = it.telefonnumre.toList(),
                )
            }
        } else {
            null
        }
    }

    class KontaktinfoRequest(
        val virksomhetsnummer: String,
    ) {
        init {
            require(virksomhetsnummer.matches(orgnrRegex))
        }

        companion object {
            private val orgnrRegex = Regex("^[0-9]{9}$")
        }
    }

    @Suppress("unused") // DTO
    class Kontaktinfo(
        val eposter: List<String>,
        val telefonnumre: List<String>,
    )

    @Suppress("unused") // DTO
    class KontaktinfoResponse(
        /* null hvis ingen tilgang */
        val hovedenhet: Kontaktinfo?,

        /* null hvis ingen tilgang */
        val underenhet: Kontaktinfo?,
    )

    companion object {
        private val ALTINN_ROLLER = setOf(
            "HADM", // Hovedadministrator
            "SIGNE", // Signerer av Samordnet registermelding
        )
        private val EXTERNAL_ROLLER = setOf(
            "DAGL", // daglig leder
            "LEDE", // styreleder
            "NEST", // nestleder
            "MEDL", // styremedlem
            "INNH", // innehaver
            "BOBE", // bobestyrer
            "BEST", // bestyrende reder
            "REPR", // norsk representant for utenlandske selskap
            "FFØR", // forretningsfører
            "KONT", // kontaktperson
        )
    }
}


