package no.nav.arbeidsgiver.min_side.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.digisyfo.*
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontostatusService
import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import no.nav.arbeidsgiver.min_side.services.tokenExchange.ClientAssertionTokenFactory
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClientImpl
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenXProperties
import no.nav.arbeidsgiver.min_side.sykefraværstatistikk.SykefraværstatistikkRepository
import no.nav.arbeidsgiver.min_side.sykefraværstatistikk.SykefraværstatistikkService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangSoknadService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssøknadClient
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import no.nav.arbeidsgiver.min_side.userinfo.UserInfoService
import no.nav.arbeidsgiver.min_side.varslingstatus.KontaktInfoPollerRepository
import no.nav.arbeidsgiver.min_side.varslingstatus.KontaktInfoPollingService
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusRepository
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension


class TilgangsstyringUtføresTest { //TODO: skirv om denne
    companion object {
        @RegisterExtension
        val app = FakeApplication(withDatabase = true) {
            provide<UserInfoService>(UserInfoService::class)
            provide(ObjectMapper::class)
            provide(AltinnService::class)

            provide(DigisyfoService::class)
            provide<DigisyfoRepository>(DigisyfoRepositoryImpl::class)
            provide<SykmeldingRepository>(SykmeldingRepositoryImpl::class)

            provide<EregClient>(EregClient::class)
            provide<EregService>(EregService::class)

            provide<KontaktInfoService>(KontaktInfoService::class)
            provide<KontaktinfoClient>(KontaktinfoClient::class)

            provide<KontostatusService>(KontostatusService::class)
            provide<KontoregisterClient>(KontoregisterClient::class)

            provide<LagredeFilterService>(LagredeFilterService::class)

            provide<RefusjonStatusService>(RefusjonStatusService::class)
            provide<RefusjonStatusRepository>(RefusjonStatusRepository::class)

            provide<TokenXProperties>(TokenXProperties::class)
            provide<TokenExchangeClient>(TokenExchangeClientImpl::class)
            provide<ClientAssertionTokenFactory>(ClientAssertionTokenFactory::class) //TODO: remove this?

            provide<SykefraværstatistikkService>(SykefraværstatistikkService::class)
            provide<SykefraværstatistikkRepository>(SykefraværstatistikkRepository::class)

            provide<AltinnTilgangssøknadClient>(AltinnTilgangssøknadClient::class)
            provide<AltinnTilgangSoknadService>(AltinnTilgangSoknadService::class)
            provide<AltinnRollerClient>(AltinnRollerClient::class)

            provide<KontaktInfoPollerRepository>(KontaktInfoPollerRepository::class)
            provide<KontaktInfoPollingService>(KontaktInfoPollingService::class)
            provide<VarslingStatusService>(VarslingStatusService::class)
            provide<VarslingStatusRepository>(VarslingStatusRepository::class)
        }
    }

    @Test
    fun tilgangsstyringUtføres() = app.runTest {
        client.get("/api/userInfo/v3")
            .let { response ->
                assert(response.status == HttpStatusCode.Unauthorized)
            }
    }

    @Test
    fun tilgangsstyringErOkForAcrLevel4() = app.runTest {
        val token = client.token(ACR.LEVEL4)
        client.get("/api/userInfo/v3") {
            bearerAuth(token)
        }
            .let { response ->
                assert(response.status == HttpStatusCode.OK)
            }
    }

    @Test
    fun tilgangsstyringErOkForAcridportenLoaHigh() = app.runTest {
        val token = client.token(ACR.IDPORTEN_LOA_HIGH)
        client.get("/api/userInfo/v3") {
            bearerAuth(token)
        }
            .let { response ->
                assert(response.status == HttpStatusCode.OK)
            }
    }

    private suspend fun HttpClient.token(acr: ACR): String =
        post("http://localhost:8118/faketokenx/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(Parameters.build {
                    append("audience", "someaudience")
                    append("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                    append("client_id", "fake")
                    append("client_secret", "fake")
                    append("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
                    append("subject_token", subjectToken)
                    append("acr", acr.value)
                })
            )
        }.body<Map<String, Any>>()["access_token"] as String
}

// subjectToken er hentet med en request mot fakedings: fakedings.intern.dev.nav.no/fake/idporten
private val subjectToken =
    "eyJraWQiOiJmYWtlIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJhdF9oYXNoIjoiNTVkODlmZjYtMjY4OS00ZWI4LWI1Y2UtNTY0MjkwNWMwMWJiIiwic3ViIjoiYWNiZGQwMmUtMDE5NS00YWIxLWEzODYtZDI5ZmZiNmIyYjVmIiwiYW1yIjpbIkJhbmtJRCJdLCJpc3MiOiJodHRwczovL2Zha2VkaW5ncy5pbnRlcm4uZGV2Lm5hdi5uby9mYWtlIiwicGlkIjoiMTIzNDU2Nzg5MTAiLCJsb2NhbGUiOiJuYiIsImNsaWVudF9pZCI6Im5vdGZvdW5kIiwidGlkIjoiZGVmYXVsdCIsInNpZCI6IjA5ZWExOWY5LTM5ODktNDM5NS1iNjE3LTJiYTA3Zjk0NWIwMyIsImF1ZCI6Im5vdGZvdW5kIiwiYWNyIjoiaWRwb3J0ZW4tbG9hLWhpZ2giLCJuYmYiOjE3MDA4NDkyNjIsImF6cCI6Im5vdGZvdW5kIiwiYXV0aF90aW1lIjoxNzAwODQ5MjYyLCJleHAiOjE3MDA4NTI4NjIsImlhdCI6MTcwMDg0OTI2MiwianRpIjoiYTYzNmVmYjMtMzgzMC00ZDU4LThmY2YtNjgwYTk3MGNjN2NlIn0.eS-FO_ty1NOANYu7IHq5mKzVjaPavpl9lrMTq7oclHJ1ymDKxpkgsslR0eLaZ6sIz9VlNPve36iIG-W_GPZmV8RvGFb6RFVORzIIKSeW1Hh3IdnRm_C5d0W_RZ48V8LolRWtM-CVs9XoVTrK9LYxfRSRz-oOTCVSjbTJVKubWNc-G7GLNpqyPM2x2pMmffMnNZz9wM_7-mxCE9KQ0lAHwS4I-xHMBYfsiezPEgmL9K-bdbMP7syMMop8BtWdaLU56VkuDO-W6DOl2plK1P9udR_h29QlHoVxwpQfUxefGbb7jeqOpVzIc45LMNEUlWxC00zfZNb3LBstopyl06ghJg"

/**
 * ifølge [doken](https://github.com/navikt/mock-oauth2-server) skal det være mulig å ha dynamiske verdier med templating i JSON_CONFIG.
 * Dette har jeg ikke fått til å fungere. Derfor er det en hardkodede verdier i JSON_CONFIG og en switch på acr requestParam
 */
private enum class ACR(val value: String) {
    LEVEL4("Level4"), IDPORTEN_LOA_HIGH("idporten-loa-high")
}

