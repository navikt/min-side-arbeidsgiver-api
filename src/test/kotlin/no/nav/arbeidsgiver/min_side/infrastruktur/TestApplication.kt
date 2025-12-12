package no.nav.arbeidsgiver.min_side.infrastruktur

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.services.digisyfo.ConsumerRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.intellij.lang.annotations.Language

/**
 * Sets up and runs a test application without a test database.
 * Allows configuration of external services, dependencies, and application setup.
 * The provided test block is executed with an HttpClient for making requests to the test application.
 * There is a separate httpClient supplied to the application dependencies, this client can be configured via [httpClientCfg].
 * The client available in the [testBlock] is configured with default JSON settings and is meant for calling the test application.
 * The httpClient provided to the application dependencies can be used to call external services. This is the client that will be mocked in tests.
 * By default, the clients are setup identically, but the httpclient provided to the application dependencies can be customized separately.
 */
fun runTestApplication(
    externalServicesCfg: ExternalServicesBuilder.() -> Unit = {},
    dependenciesCfg: DependencyRegistry.(ApplicationTestBuilder) -> Unit = {},
    applicationCfg: suspend Application.() -> Unit = {},
    httpClientCfg: (HttpClientConfig<*>.() -> Unit)? = null,
    testBlock: suspend ApplicationTestBuilder.() -> Unit
) = testApplication {
    configureTestApp(
        externalServicesCfg = externalServicesCfg,
        dependenciesCfg = dependenciesCfg,
        applicationCfg = applicationCfg,
        httpClientCfg = httpClientCfg,
    )
    startApplication()
    testBlock()
}

/**
 * Sets up and runs a test application with a test database.
 * The test database is created, migrated, and provided to the application dependencies.
 * After the test block is executed, the test database is cleaned up.
 * Allows configuration of external services, dependencies, and application setup.
 * The provided test block is executed with an HttpClient for making requests to the test application.
 * There is a separate httpClient supplied to the application dependencies, this client can be configured via [httpClientCfg].
 * The client available in the [testBlock] is configured with default JSON settings and is meant for calling the test application.
 * The httpClient provided to the application dependencies can be used to call external services. This is the client that will be mocked in tests.
 * By default, the clients are setup identically, but the httpclient provided to the application dependencies can be customized separately.
 */
fun runTestApplicationWithDatabase(
    externalServicesCfg: ExternalServicesBuilder.() -> Unit = {},
    dependenciesCfg: DependencyRegistry.(ApplicationTestBuilder) -> Unit = {},
    applicationCfg: suspend Application.() -> Unit = {},
    httpClientCfg: (HttpClientConfig<*>.() -> Unit)? = null,
    testBlock: suspend ApplicationTestBuilder.() -> Unit
) = testApplicationWithDatabase { testDatabase ->
    configureTestApp(
        externalServicesCfg = externalServicesCfg,
        dependenciesCfg = {
            provide<Database> { testDatabase }
            dependenciesCfg(this@testApplicationWithDatabase)
        },
        applicationCfg = applicationCfg,
        httpClientCfg = httpClientCfg,
    )
    startApplication()
    testBlock()
}

val ApplicationTestBuilder.database: Database
    get() = runBlocking {
        application.dependencies.resolve()
    }

suspend inline fun <reified T> ApplicationTestBuilder.resolve(key: String? = null): T =
    application.dependencies.resolve(key)

private fun ApplicationTestBuilder.configureTestApp(
    externalServicesCfg: ExternalServicesBuilder.() -> Unit,
    dependenciesCfg: DependencyRegistry.(ApplicationTestBuilder) -> Unit,
    applicationCfg: suspend Application.() -> Unit,
    httpClientCfg: (HttpClientConfig<*>.() -> Unit)?,
) {
    externalServices {
        externalServicesCfg()
    }
    client = createClient {
        install(ContentNegotiation) {
            json(defaultJson)
        }
    }
    val httpClient = createClient {
        if (httpClientCfg != null) {
            httpClientCfg()
        } else {
            install(ContentNegotiation) {
                json(defaultJson)
            }
        }
    }
    application {
        dependencies {
            provide<HttpClient> { httpClient }
            dependenciesCfg(this@configureTestApp)
        }
        applicationCfg()
    }
}

suspend fun ConsumerRecordProcessor.processRecordValue(@Language("JSON") value: String) = processRecord(
    ConsumerRecord("", 0, 0, "", value)
)

suspend fun ConsumerRecordProcessor.processRecordsValue(@Language("JSON") value: String) =
    processRecordsValues(listOf(value))

suspend fun ConsumerRecordProcessor.processRecordsValues(
    @Language("JSON") values: List<String>
) = processRecords(
    ConsumerRecords(
        mapOf(
            TopicPartition("topic", 1) to values.map {
                ConsumerRecord("topic", 1, 0, "someid", it)
            }
        )
    )
)

suspend fun ConsumerRecordProcessor.processRecordKeyValue(
    @Language("JSON") key: String,
    @Language("JSON") value: String
) = processRecord(
    ConsumerRecord("", 0, 0, key, value)
)