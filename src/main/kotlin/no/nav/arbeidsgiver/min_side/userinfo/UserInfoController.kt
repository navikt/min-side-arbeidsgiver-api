package no.nav.arbeidsgiver.min_side.userinfo

import com.fasterxml.jackson.annotation.JsonIgnore
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
            val tilganger = tjenester.map { (id, tjeneste) ->
                async {
                    runCatching {
                        altinnService.hentOrganisasjonerBasertPaRettigheter(
                            authenticatedUserHolder.fnr,
                            tjeneste.tjenestekode,
                            tjeneste.tjenesteversjon
                        )
                    }.fold(
                        onSuccess = {
                            UserInfoRespons.Tilgang(
                                id = id,
                                tjenestekode = tjeneste.tjenestekode,
                                tjenesteversjon = tjeneste.tjenesteversjon,
                                organisasjoner = it.mapNotNull { it.organizationNumber },
                                altinnError = false,
                            )
                        },
                        onFailure = {
                            UserInfoRespons.Tilgang(
                                id = id,
                                tjenestekode = tjeneste.tjenestekode,
                                tjenesteversjon = tjeneste.tjenesteversjon,
                                organisasjoner = emptyList(),
                                altinnError = true,
                            )
                        }
                    )
                }
            }.awaitAll()

            val organisasjoner = async {
                runCatching {
                    altinnService.hentOrganisasjoner(authenticatedUserHolder.fnr)
                        .filter {
                            it.organizationForm == "BEDR"
                                    || it.organizationForm == "AAFY"
                                    || it.type == "Enterprise"
                        }
                }
            }.await()

            tilganger to organisasjoner
        }

        return UserInfoRespons(
            altinnError = organisasjoner.isFailure || tilganger.any { it.altinnError },
            organisasjoner = organisasjoner.getOrDefault(emptyList()),
            tilganger = tilganger
        )
    }

    data class UserInfoRespons(
        val organisasjoner: List<Organisasjon>,
        val tilganger: List<Tilgang>,
        val altinnError: Boolean,
    ) {
        data class Tilgang(
            val id: String,
            val tjenestekode: String,
            val tjenesteversjon: String,
            val organisasjoner: List<String>,
            @get:JsonIgnore val altinnError: Boolean,
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
