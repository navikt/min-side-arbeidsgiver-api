package no.nav.arbeidsgiver.min_side

import com.codahale.metrics.MetricRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import org.flywaydb.core.Flyway
import java.sql.Connection

class Database private constructor(private val config: DatabaseConfig) {
    private val dataSource = HikariDataSource(config.asHikariConfig())

    private suspend fun migrate() {
        withContext(Dispatchers.IO) {
            Flyway.configure()
                .locations("")
                .dataSource(dataSource)
                .load()
                .migrate()
        }
    }

    companion object {
        fun CoroutineScope.openDatabaseAsync(config: DatabaseConfig): Deferred<Database> {
            return async {
                val database = Database(config)
                database.migrate()
                database
            }
        }
    }
}

data class DatabaseConfig(
    val jdbcUrl: String,
    val migrationLocation: String,
) {
    fun asHikariConfig(): HikariConfig {
        return HikariConfig().apply {
            jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
            metricRegistry = MetricRegistry()
            minimumIdle = 1
            maximumPoolSize = 10
            connectionTimeout = 10000
            idleTimeout = 10001
            maxLifetime = 30001
            leakDetectionThreshold = 30000
            isAutoCommit = false
        }
    }
}




