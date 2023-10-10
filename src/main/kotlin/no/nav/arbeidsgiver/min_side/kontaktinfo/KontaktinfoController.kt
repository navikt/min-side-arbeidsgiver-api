package no.nav.arbeidsgiver.min_side.kontaktinfo

import org.springframework.web.bind.annotation.*

@RestController
class KontaktinfoController {

    @PostMapping("/api/kontaktinfo/v1")
    fun getKontaktinfo(@RequestBody requestBody: KontaktinfoRequest): KontaktinfoResponse {
        /* TODO tilgangsstyring */
        /* TODO hente kofuvi */

        return KontaktinfoResponse(
            underenhet = null,
            hovedenhet = null,
        )
    }

    class KontaktinfoRequest(
        val virksomhetsnummer: String,
    )

    @Suppress("unused")
    class Kontaktinfo(
        val eposter: List<String>,
        val telefonnummer: List<String>,
    )

    @Suppress("unused")
    class KontaktinfoResponse(
        /* null hvis ingen tilgang */
        val hovedenhet: Kontaktinfo?,

        /* null hvis ingen tilgang */
        val underenhet: Kontaktinfo?,
    )
}


