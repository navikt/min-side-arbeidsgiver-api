package no.nav.arbeidsgiver.min_side

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.core.JsonProcessingException
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.Database.Companion.openDatabaseAsync
import no.nav.arbeidsgiver.min_side.azuread.AzureAdConfig
import no.nav.arbeidsgiver.min_side.azuread.AzureClient
import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.maskinporten.*
import org.slf4j.event.Level
import java.util.*


private val databaseConfig = DatabaseConfig(
    jdbcUrl = System.getenv("JDBC_DATABASE_URL"), // fix this based on env
    migrationLocation = "classpath:db/migration"
)

private val maskinportenConfig = MaskinportenConfig2(
    scopes = System.getenv("MASKINPORTEN_SCOPES"),
    wellKnownUrl = System.getenv("MASKINPORTEN_WELL_KNOWN_URL"),
    clientId = System.getenv("MASKINPORTEN_CLIENT_ID"),
    clientJwk = System.getenv("MASKINPORTEN_CLIENT_JWK"),
)

private val azureAdConfig = AzureAdConfig(
    openidTokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    clientId = System.getenv("AZURE_APP_CLIENT_ID"),
    clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
)

fun main() {
    runBlocking(Dispatchers.Default) {
        embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
            ktorConfig()
            configureDependencies()
        }
    }
}

fun Application.configureDependencies() {
    dependencies {
        provide<Database> { openDatabaseAsync(databaseConfig).await() }

        provide(MeterRegistry::class)

        provide<MaskinportenClient> { MaskinportenClientImpl(maskinportenConfig) }
        provide<MaskinportenTokenService>(MaskinportenTokenServiceImpl::class)

        provide<AzureClient> { AzureClient(azureAdConfig) }
        provide(AzureService::class)


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
        mdc("clientId") { call ->
            call.principal<InnloggetBrukerPrincipal>()?.clientId //TODO: fiks denne når principal er på plass
        }
        callIdMdc("x_correlation_id")
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
        jwt {
            verifier(
                JWT
                    .require(Algorithm.HMAC256("secret"))
                    .withIssuer(System.getenv("TOKEN_X_ISSUER"))
                    .withAudience(System.getenv("TOKEN_X_CLIENT_ID"))
                    .build()
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
        json()
    }
}