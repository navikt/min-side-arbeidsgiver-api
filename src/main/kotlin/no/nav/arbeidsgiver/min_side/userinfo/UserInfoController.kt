package no.nav.arbeidsgiver.min_side.userinfo

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import no.nav.arbeidsgiver.min_side.config.GittMiljø
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserInfoController(
    private val altinnService: AltinnService,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
    gittMiljø: GittMiljø,
) {

    val tjenester = mapOf(
        "ekspertbistand" to Altinnskjema(
            tjenestekode = "5384",
            tjenesteversjon = "1",
        ),
        "inntektsmelding" to Altinnskjema(
            tjenestekode = "4936",
            tjenesteversjon = "1",
        ),
        "utsendtArbeidstakerEØS" to Altinnskjema(
            tjenestekode = "4826",
            tjenesteversjon = "1",
        ),
        "arbeidstrening" to NAVTjeneste(
            tjenestekode = "5332",
            tjenesteversjon = gittMiljø.resolve(
                prod = { "2" },
                other = { "1" },
            ),
        ),
        "arbeidsforhold" to NAVTjeneste(
            tjenestekode = "5441",
            tjenesteversjon = "1",
        ),
        "midlertidigLønnstilskudd" to NAVTjeneste(
            tjenestekode = "5516",
            tjenesteversjon = "1",
        ),
        "varigLønnstilskudd" to NAVTjeneste(
            tjenestekode = "5516",
            tjenesteversjon = "2",
        ),
        "sommerjobb" to NAVTjeneste(
            tjenestekode = "5516",
            tjenesteversjon = "3",
        ),
        "mentortilskudd" to NAVTjeneste(
            tjenestekode = "5516",
            tjenesteversjon = "4",
        ),
        "inkluderingstilskudd" to NAVTjeneste(
            tjenestekode = "5516",
            tjenesteversjon = "5",
        ),
        "sykefravarstatistikk" to NAVTjeneste(
            tjenestekode = "3403",
            tjenesteversjon = gittMiljø.resolve(
                prod = { "2" },
                other = { "1" },
            ),
        ),
        "forebyggefravar" to NAVTjeneste(
            tjenestekode = "5934",
            tjenesteversjon = "1",
        ),
        "rekruttering" to NAVTjeneste(
            tjenestekode = "5078",
            tjenesteversjon = "1",
        ),
        "tilskuddsbrev" to NAVTjeneste(
            tjenestekode = "5278",
            tjenesteversjon = "1",
        ),
        "yrkesskade" to NAVTjeneste(
            tjenestekode = "5902",
            tjenesteversjon = "1",
        ),
    )

    @GetMapping("/api/userInfo/v1")
    suspend fun getUserInfo(): UserInfoRespons {
        val (tilganger, organisasjoner) = supervisorScope {
            val tjenester = tjenester.map { (id, tjeneste) ->
                async {
                    runCatching {
                        val organisasjonerBasertPaRettigheter = altinnService.hentOrganisasjonerBasertPaRettigheter(
                            authenticatedUserHolder.fnr,
                            tjeneste.tjenestekode,
                            tjeneste.tjenesteversjon
                        )

                        if (organisasjonerBasertPaRettigheter.isEmpty()) {
                            null
                        } else {
                            UserInfoRespons.Tilgang(
                                id = id,
                                tjenestekode = tjeneste.tjenestekode,
                                tjenesteversjon = tjeneste.tjenesteversjon,
                                organisasjoner = organisasjonerBasertPaRettigheter
                            )
                        }
                    }
                }
            }.awaitAll()

            val organisasjoner = async {
                runCatching {
                    altinnService.hentOrganisasjoner(authenticatedUserHolder.fnr)
                }
            }.await()

            tjenester to organisasjoner
        }

        return UserInfoRespons(
            altinnError = organisasjoner.isFailure || tilganger.any { it.isFailure },
            organisasjoner = organisasjoner.getOrDefault(emptyList()),
            tilganger = tilganger.mapNotNull { it.getOrNull() },
        )
    }

    data class UserInfoRespons(
        val organisasjoner: List<Organisasjon>,
        val tilganger: List<Tilgang>,
        val altinnError: Boolean = false,
    ) {
        data class Tilgang(
            val id: String,
            val tjenestekode: String,
            val tjenesteversjon: String,
            val organisasjoner: List<Organisasjon>,
        )
    }

    sealed interface Tjeneste {
        val sort: Sort
        val tjenestekode: String
        val tjenesteversjon: String
    }

    data class NAVTjeneste(
        override val tjenestekode: String,
        override val tjenesteversjon: String,
    ) : Tjeneste {
        override val sort = Sort.tjeneste
    }

    data class Altinnskjema(
        override val tjenestekode: String,
        override val tjenesteversjon: String,
    ) : Tjeneste {
        override val sort = Sort.skjema
    }

    enum class Sort { tjeneste, skjema }
}
