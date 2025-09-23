package no.nav.arbeidsgiver.min_side

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.di.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.Database.Companion.openDatabase
import no.nav.arbeidsgiver.min_side.azuread.AzureAdConfig
import no.nav.arbeidsgiver.min_side.azuread.AzureClient
import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.config.Environment
import no.nav.arbeidsgiver.min_side.config.GittMiljø2
import no.nav.arbeidsgiver.min_side.config.MsaJwtVerifier
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.maskinporten.*
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
import no.nav.arbeidsgiver.min_side.varslingstatus.*
import org.slf4j.event.Level
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json
import java.util.*


fun main() {
    runBlocking(Dispatchers.Default) {
        embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
            ktorConfig()
            configureDependencies()
            configureRoutes()

            startKafkaConsumers(this)
            startKontaktInfoPollingServices(this)
            startDeleteOldSykmeldingLoop(this)

            // Maskinporten token refresher
            launch {
                dependencies.resolve<MaskinportenTokenService>().tokenRefreshingLoop()
            }
        }.start(wait = false)
    }
}

fun Application.configureRoutes() {
    install(IgnoreTrailingSlash)
    routing {
        authenticate("jwt") {
            // Kontaktinfo
            post("/api/kontaktinfo/v1") {
                call.respond(
                    dependencies.resolve<KontaktInfoService>()
                        .getKontaktinfo(call.receive(), AuthenticatedUserHolder(call))
                )
            }

            // Kontonummer
            post("/api/kontonummerStatus/v1") {
                call.respond(
                    dependencies.resolve<KontostatusService>().getKontonummerStatus(call.receive())
                )
            }
            post("/api/kontonummer/v1") {
                call.respond(
                    dependencies.resolve<KontostatusService>()
                        .getKontonummer(
                            call.receive(), AuthenticatedUserHolder(call)
                        ) ?: HttpStatusCode.NotFound
                )
            }

            // Lagrede filter
            get("/api/lagredeFilter") {
                val service = dependencies.resolve<LagredeFilterService>()
                call.respond(
                    dependencies.resolve<LagredeFilterService>()
                        .getAll(authenticatedUserHolder = AuthenticatedUserHolder(call))
                )
            }
            put("/api/lagredeFilter") {
                call.respond(
                    dependencies.resolve<LagredeFilterService>()
                        .put(call.receive(), authenticatedUserHolder = AuthenticatedUserHolder(call))
                )
            }
            delete("/api/lagredeFilter/{filterId}") {
                call.respond(
                    dependencies.resolve<LagredeFilterService>().delete(
                        call.parameters["filterId"]!!,
                        authenticatedUserHolder = AuthenticatedUserHolder(call)
                    ) ?: HttpStatusCode.NotFound
                )
            }

            // Ereg
            post("api/ereg/underenhet") {
                call.respond(dependencies.resolve<EregService>().underenhet(call.receive()) ?: HttpStatusCode.NotFound)
            }
            post("api/ereg/overenhet") {
                call.respond(dependencies.resolve<EregService>().overenhet(call.receive()) ?: HttpStatusCode.NotFound)
            }

            // Refusjon status
            get("/api/refusjon_status") {
                call.respond(
                    dependencies.resolve<RefusjonStatusService>().statusoversikt(AuthenticatedUserHolder(call))
                )
            }

            // Sykefraværstatistikk
            get("/api/sykefravaerstatistikk/{orgnr}") {
                call.respond(
                    dependencies.resolve<SykefraværstatistikkService>()
                        .getStatistikk(call.parameters["orgnr"]!!, AuthenticatedUserHolder(call))
                )
            }

            // Tilgangsøknad
            get("/api/altinn-tilgangssoknad") {
                call.respond(
                    dependencies.resolve<AltinnTilgangSoknadService>().mineSøknaderOmTilgang(
                        AuthenticatedUserHolder(call)
                    )
                )
            }
            post("/api/altinn-tilgangssoknad") {
                call.respond(
                    dependencies.resolve<AltinnTilgangSoknadService>()
                        .sendSøknadOmTilgang(call.receive(), authenticatedUserHolder = AuthenticatedUserHolder(call))
                )
            }

            // Userinfo
            get("/api/userInfo/v3") {
                call.respond(dependencies.resolve<UserInfoService>().getUserInfoV3(AuthenticatedUserHolder(call)))
            }

            // Varsling status
            post("/api/varslingStatus/v1") {
                call.respond(
                    dependencies.resolve<VarslingStatusService>()
                        .getVarslingStatus(call.receive(), AuthenticatedUserHolder(call))
                )
            }
        }
    }
}

fun Application.configureDependencies() {
    val databaseConfig = DatabaseConfig(
        jdbcUrl = System.getenv("JDBC_DATABASE_URL"), // fix this based on env
        migrationLocation = "classpath:db/migration"
    )

    val maskinportenConfig = MaskinportenConfig2(
        scopes = System.getenv("MASKINPORTEN_SCOPES"),
        wellKnownUrl = System.getenv("MASKINPORTEN_WELL_KNOWN_URL"),
        clientId = System.getenv("MASKINPORTEN_CLIENT_ID"),
        clientJwk = System.getenv("MASKINPORTEN_CLIENT_JWK"),
    )

    val azureAdConfig = AzureAdConfig(
        openidTokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = System.getenv("AZURE_APP_CLIENT_ID"),
        clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
    )

    dependencies {
        provide<Database> { openDatabase(databaseConfig) }

        provide(MeterRegistry::class)
        provide(ObjectMapper::class)

        provide<MaskinportenClient> { MaskinportenClientImpl(maskinportenConfig) }
        provide<MaskinportenTokenService>(MaskinportenTokenServiceImpl::class)

        provide { AzureClient(azureAdConfig) }
        provide(AzureService::class)
        provide(AltinnService::class)

        provide<DigisyfoRepository>(DigisyfoRepositoryImpl::class)
        provide(DigisyfoService::class)
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


fun Application.ktorConfig() {
    val log = logger()

    log.info("Starting ktor application")

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/internal/") }
        System.getenv("NAIS_CLUSTER_NAME")?.let {
            disableDefaultColors()
        }
        mdc("method") { call ->
            call.request.httpMethod.value
        }
        mdc("host") { call ->
            call.request.header("host")
        }
        mdc("path") { call ->
            call.request.path()
        }
        callIdMdc(HttpHeaders.XCorrelationId)
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        retrieveFromHeader("call-id")
        retrieveFromHeader("callId")
        retrieveFromHeader("call_id")

        generate {
            UUID.randomUUID().toString()
        }

        replyToHeader(HttpHeaders.XCorrelationId)
    }

    install(Authentication) {
        jwt("jwt") {
            verifier(
                MsaJwtVerifier()
            )
            validate {
                val validAcrClaims = listOf("Level4", "idporten-loa-high")
                it.payload.getClaim("acr").let { claim ->
                    if (validAcrClaims.contains(claim.asString()))
                        JWTPrincipal(it.payload)
                    else
                        null
                }
            }
        }
    }

    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    install(StatusPages) {
        exception<BadRequestException> { call, ex ->
            log.warn("unhandled exception in ktor pipeline: {}", ex::class.qualifiedName, ex)
            call.respond(
                HttpStatusCode.InternalServerError, mapOf(
                    "error" to "unexpected error",
                )
            )
        }

        exception<JsonProcessingException> { call, ex ->
            ex.clearLocation()
            log.error("unhandled exception in ktor pipeline: {}", ex::class.qualifiedName, ex)
            call.respond(
                HttpStatusCode.InternalServerError, mapOf(
                    "error" to "unexpected error",
                )
            )
        }

        exception<Throwable> { call, ex ->
            log.error("unhandled exception in ktor pipeline: {}", ex::class.qualifiedName, ex)
            call.respond(
                HttpStatusCode.InternalServerError, mapOf(
                    "error" to "unexpected error",
                )
            )
        }
    }

    install(ContentNegotiation) {
        jackson()
    }
}