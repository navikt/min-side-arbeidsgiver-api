package no.nav.arbeidsgiver.min_side

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
}