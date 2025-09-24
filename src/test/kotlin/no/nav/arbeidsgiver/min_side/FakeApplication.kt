package no.nav.arbeidsgiver.min_side

import io.ktor.client.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
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
import org.junit.jupiter.api.extension.*
import org.slf4j.event.Level
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FakeApplication(
    port: Int = 0,
    dependencyConfiguration: DependencyRegistry.() -> Unit
) : BeforeAllCallback, AfterAllCallback {
    private val server = embeddedServer(CIO, port = port, host = "localhost") {
        ktorConfig()
        configureRoutes()
        routing {
            get("/tokenIntrospection") {
                call.respond(MsaJwtVerifier.TokenIntrospectionResponse(active = true, null))
            }
        }
        dependencies {
            dependencyConfiguration()
        }
    }
    private val testContext = TestContext()

    override fun beforeAll(context: ExtensionContext?) {
        server.start(wait = false)
    }

    override fun afterAll(context: ExtensionContext?) {
        server.stop()
    }



    fun runTest(block: suspend TestContext.() -> Unit) = runBlocking {
        block(testContext)
    }
}

class TestContext{
    val client = defaultHttpClient(configure = {
        install(DefaultRequest) {
            url("http://localhost:8080")
        }
    })
}

class FakeApi : BeforeTestExecutionCallback, AfterTestExecutionCallback {
    val stubs = mutableMapOf<Pair<HttpMethod, String>, (suspend RoutingContext.(Any) -> Unit)>()

    val errors = mutableListOf<Throwable>()

    private val server = embeddedServer(CIO, port = 0) {
        install(CallLogging) {
            level = Level.INFO
        }

        install(ContentNegotiation) {
            json()
        }

        routing {
            get("/internal/isready") {
                call.response.status(HttpStatusCode.OK)
            }

            post("{...}") {
                stubs[HttpMethod.Post to call.request.path()]?.let { handler ->
                    try {
                        handler(this)
                    } catch (e: Exception) {
                        errors.add(e)
                        throw e
                    }
                } ?: return@post call.response.status(HttpStatusCode.NotFound)
            }

            get("{...}") {
                stubs[HttpMethod.Get to call.request.path()]?.let { handler ->
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

    override fun beforeTestExecution(context: ExtensionContext?) {
        runBlocking {
            server.start(wait = false)
            server.engine.waitUntilReady()

        }
    }


    override fun afterTestExecution(context: ExtensionContext?) {
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
