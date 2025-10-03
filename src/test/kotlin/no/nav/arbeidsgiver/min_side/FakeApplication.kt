package no.nav.arbeidsgiver.min_side

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.config.MsaJwtVerifier
import no.nav.arbeidsgiver.min_side.config.logger
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.event.Level
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FakeApplication(
    addDatabase: Boolean = false,
    configure: suspend Application.() -> Unit
) : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
    private var database: TestDatabase? = null

    private val server = embeddedServer(CIO, port = 8080, host = "localhost") {
        ktorConfig()
        configureRoutes()
        dependencies {
            if (addDatabase) {
                database = TestDatabase()
                database!!.migrate()
                provide<Database> { database!! }
            }
        }
        configure()
    }
    private val testContext = TestContext()

    internal suspend inline fun <reified T> getDependency(): T {
        return this.server.application.dependencies.resolve<T>()
    }

    fun runTest(block: suspend TestContext.() -> Unit) = runBlocking {
        block(testContext)
    }

    override fun beforeAll(context: ExtensionContext?) {
        server.start(wait = false)
    }

    override fun afterAll(context: ExtensionContext?) {
        server.stop()
        database?.close()
    }

    override fun beforeEach(context: ExtensionContext?) {
        database?.clean()
        database?.migrate()
    }
}

class TestContext {
    val client = defaultHttpClient(configure = {
        install(DefaultRequest) {
            url("http://localhost:8080") // Default port for FakeApi
        }
    })
}

class FakeApi : BeforeAllCallback, AfterAllCallback {
    private val stubs = mutableMapOf<Pair<HttpMethod, String>, (suspend RoutingContext.(Any) -> Unit)>()
    val errors = mutableListOf<Throwable>()

    fun registerStub(
        method: HttpMethod,
        path: String,
        queryParams: Parameters = Parameters.Empty,
        handler: suspend RoutingContext.(Any) -> Unit
    ) {
        val pathAndQuery = path + if (!queryParams.isEmpty()) {
            "?" + queryParams.entries().joinToString("&") { entry ->
                entry.value.joinToString("&") { value ->
                    if (value.isEmpty()) {
                        entry.key
                    } else {
                        "${entry.key}=$value"
                    }
                }
            }
        } else {
            ""
        }
        stubs[method to pathAndQuery] = handler
    }

    private fun pathWithQuery(call: ApplicationCall): String {
        val query = call.request.queryString().let { if (it.isNotEmpty()) "?$it" else it }
        return call.request.path() + query
    }

    private val server = embeddedServer(CIO, port = 8081, host = "localhost") {
        install(CallLogging) {
            level = Level.INFO
        }

        install(ContentNegotiation) {
            jackson()
        }

        routing {
            get("/internal/isready") {
                call.response.status(HttpStatusCode.OK)
            }

            get("/tokenIntrospection") {
                call.respond(MsaJwtVerifier.TokenIntrospectionResponse(active = true, null))
            }

            post("{...}") {
                (stubs[HttpMethod.Post to pathWithQuery(call)] // Check if there is match with query params, otherwise fallback to path only
                    ?: stubs[HttpMethod.Post to call.request.path()])?.let { handler ->
                    try {
                        handler(this)
                    } catch (e: Exception) {
                        errors.add(e)
                        throw e
                    }
                } ?: return@post call.response.status(HttpStatusCode.NotFound)
            }

            get("{...}") {
                (stubs[HttpMethod.Get to pathWithQuery(call)]
                    ?: stubs[HttpMethod.Get to call.request.path()])?.let { handler ->
                    try {
                        handler(this)
                    } catch (e: Exception) {
                        errors.add(e)
                        throw e
                    }
                } ?: return@get call.response.status(HttpStatusCode.NotFound)
            }
        }
    }

    suspend fun ApplicationEngine.waitUntilReady() {
        val log = logger()

        val client = HttpClient(io.ktor.client.engine.cio.CIO)
        suspend fun isAlive() = runCatching {
            val port = resolvedConnectors().first().port
            client.get("http://localhost:$port/internal/isready").status == HttpStatusCode.OK
        }.getOrElse {
            log.warn("not alive yet: $it", it)
            false
        }

        while (!isAlive()) {
            delay(100)
        }
    }

    override fun beforeAll(context: ExtensionContext?) {
        runBlocking {
            server.start(wait = false)
            server.engine.waitUntilReady()
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        server.stop()
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun fakeToken(pid: String): String {
    val header = """
            {
                "alg": "HS256",
                "typ": "JWT"
            }
        """.trimIndent()
    val payload = """
            {
              "active": true,
              "pid": "$pid",
              "acr": "idporten-loa-high"
            }
            """.trimIndent()
    val secret = "secret"

    fun String.b64Url(): String =
        Base64.UrlSafe.encode(this.encodeToByteArray()).trimEnd('=')

    val value = "${header.b64Url()}.${payload.b64Url()}.${secret.b64Url()}"
    return value
}