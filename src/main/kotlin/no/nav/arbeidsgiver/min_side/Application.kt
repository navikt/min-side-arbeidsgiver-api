package no.nav.arbeidsgiver.min_side

import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.infrastruktur.Database.Companion.openDatabase
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.digisyfo.*
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregClientImpl
import no.nav.arbeidsgiver.min_side.services.ereg.EregService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClientImpl
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClientImpl
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontostatusService
import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusServiceImpl
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.SykefravarstatistikkRepository
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.SykefraværstatistikkService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangSoknadService
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClient
import no.nav.arbeidsgiver.min_side.tilgangssoknad.AltinnTilgangssoknadClientImpl
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClientImpl
import no.nav.arbeidsgiver.min_side.userinfo.UserInfoService
import no.nav.arbeidsgiver.min_side.varslingstatus.*
import org.slf4j.event.Level
import java.util.*

fun main() {
    embeddedServer(
        CIO,
        configure = {
            connector {
                port = 8080
                host = "0.0.0.0"
            }
            shutdownGracePeriod = 20_000
            shutdownTimeout = 30_000
        }
    ) {
        ktorConfig()
        configureDependencies()
        configureTokenXAuth()
        configureRoutes()

        startKafkaConsumers(CoroutineScope(coroutineContext + Dispatchers.IO.limitedParallelism(3)))
        startKontaktInfoPollingServices(CoroutineScope(coroutineContext + Dispatchers.IO.limitedParallelism(3)))
        startDeleteOldSykmeldingLoop(CoroutineScope(coroutineContext + Dispatchers.IO.limitedParallelism(1)))

        registerShutdownListener()
    }.start(wait = true)
}


internal val RoutingContext.subjectToken
    get() = call.principal<TokenXPrincipal>()!!.subjectToken

internal val RoutingContext.innloggetBruker
    get() = call.principal<TokenXPrincipal>()!!.pid

suspend fun Application.configureRoutes() {
    configureInternalRoutes()
    configureKontaktinfoRoutes()
    configureKontonummerRoutes()
    configureLagredefilterRoutes()
    configureEregRoutes()
    configureSykefravarstatistikkRoutes()
    configureTilgangssoknadRoutes()
    configureUserInfoRoutes()
    configureVarslingStatusRoutes()
}

fun Application.configureDependencies() {
    val databaseConfig = DatabaseConfig(
        jdbcUrl = DbUrl(System.getenv("DB_JDBC_URL")).jdbcUrl, // Vi får en dårlig jdbcUrl fra nais, så vi må vaske denne. Dette vil bli fikset dersom vi rullerer secrets
        migrationLocation = "classpath:db/migration"
    )
    dependencies {
        provide<Database> { openDatabase(databaseConfig) }

        provide<AuthConfig> { AuthConfig.nais }
        provide<HttpClient> { defaultHttpClient() }

        provide<TokenXTokenIntrospector>(TokenXAuthClient::class)
        provide<TokenXTokenExchanger>(TokenXAuthClient::class)
        provide<MaskinportenTokenProvider>(MaskinportenAuthClient::class)
        provide<AzureAdTokenProvider>(AzureAdAuthClient::class)

        provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)

        provide<KontaktinfoClient>(KontaktinfoClientImpl::class)

        provide<AltinnRollerClient>(AltinnRollerClientImpl::class)

        provide<EregClient>(EregClientImpl::class)

        provide<KontaktInfoService>(KontaktInfoService::class)

        provide<EregService>(EregService::class)

        provide<KontoregisterClient>(KontoregisterClientImpl::class)

        provide<DigisyfoRepository>(DigisyfoRepositoryImpl::class)
        provide<DigisyfoService>(DigisyfoServiceImpl::class)
        provide<SykmeldingRepository>(SykmeldingRepositoryImpl::class)

        provide<UserInfoService>(UserInfoService::class)

        provide<KontostatusService>(KontostatusService::class)

        provide<LagredeFilterService>(LagredeFilterService::class)

        provide<RefusjonStatusService>(RefusjonStatusServiceImpl::class)
        provide<RefusjonStatusRepository>(RefusjonStatusRepository::class)

        provide<SykefraværstatistikkService>(SykefraværstatistikkService::class)
        provide<SykefravarstatistikkRepository>(SykefravarstatistikkRepository::class)

        provide<AltinnTilgangssoknadClient>(AltinnTilgangssoknadClientImpl::class)
        provide<AltinnTilgangSoknadService>(AltinnTilgangSoknadService::class)

        provide<KontaktInfoPollerRepository>(KontaktInfoPollerRepository::class)
        provide<KontaktInfoPollingService>(KontaktInfoPollingService::class)
        provide<VarslingStatusService>(VarslingStatusService::class)
        provide<VarslingStatusRepository>(VarslingStatusRepository::class)
    }
}


fun Application.ktorConfig() {
    log.info("Starting ktor application")

    install(IgnoreTrailingSlash)

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
        disableDefaultColors()
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

    install(MicrometerMetrics) {
        registry = Metrics.meterRegistry
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .build()
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            FileDescriptorMetrics(),
            LogbackMetrics()
        )
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException,
                is BadRequestException -> call.respondText(
                    text = "${HttpStatusCode.BadRequest}: $cause",
                    status = HttpStatusCode.BadRequest
                )

                is HttpRequestTimeoutException,
                is ConnectTimeoutException -> {
                    call.application.log.warn(
                        "Unexpected exception at ktor-toplevel: {}",
                        cause.javaClass.canonicalName,
                        cause
                    )
                    call.response.status(HttpStatusCode.InternalServerError)
                }

                else -> {
                    call.application.log.error(
                        "Unexpected exception at ktor-toplevel: {}",
                        cause.javaClass.canonicalName,
                        cause
                    )
                    call.response.status(HttpStatusCode.InternalServerError)
                }
            }
        }
    }

    install(ContentNegotiation) {
        json(defaultJson)
    }
}

fun Application.configureInternalRoutes() {
    routing {
        route("internal") {
            get("prometheus") {
                call.respond<String>(Metrics.meterRegistry.scrape())
            }
            get("isalive") {
                call.response.status(
                    if (Health.alive)
                        HttpStatusCode.OK
                    else
                        HttpStatusCode.ServiceUnavailable
                )
            }
            get("isready") {
                call.response.status(
                    if (Health.ready)
                        HttpStatusCode.OK
                    else
                        HttpStatusCode.ServiceUnavailable
                )
            }
        }
    }
}

fun Application.msaApiRouting(build: Route.() -> Unit) {
    routing {
        authenticate(TOKENX_PROVIDER) {
            route("ditt-nav-arbeidsgiver-api/api") {
                build()
            }
        }
    }
}