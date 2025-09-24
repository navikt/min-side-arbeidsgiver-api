import no.nav.arbeidsgiver.min_side.Database
import no.nav.arbeidsgiver.min_side.DatabaseConfig
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext


val testDatabaseConfig = DatabaseConfig(
    jdbcUrl = "jdbc:postgresql://localhost:2345/?password=postgres&user=postgres",
    migrationLocation = "db/migration"
)

class TestDatabase() : Database(testDatabaseConfig), BeforeEachCallback {
    private val flyway = Flyway.configure()
        .locations(config.migrationLocation)
        .dataSource(dataSource)
        .cleanDisabled(false)
        .load()

    override fun beforeEach(context: ExtensionContext?) {
        flyway.clean()
        flyway.migrate()
    }
}