package no.nav.arbeidsgiver.min_side.infrastruktur

import io.ktor.server.testing.*
import org.flywaydb.core.Flyway

val testDatabaseConfig = DatabaseConfig(
    jdbcUrl = "jdbc:postgresql://localhost:2345/?password=postgres&user=postgres",
    migrationLocation = "db/migration"
)

class TestDatabase() : Database(testDatabaseConfig) {
    private val flyway = Flyway.configure()
        .locations(config.migrationLocation)
        .dataSource(dataSource)
        .cleanDisabled(false)
        .load()

    fun migrate() {
        flyway.migrate()
    }

    fun clean() {
        flyway.clean()
    }

    fun cleanMigrate(): TestDatabase = also {
        clean()
        migrate()
    }
}

fun testApplicationWithDatabase(
    block: suspend ApplicationTestBuilder.(testDatabase: TestDatabase) -> Unit
) = testApplication {
    TestDatabase().cleanMigrate().use { testDatabase ->
        block(testDatabase)
    }
}