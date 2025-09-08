package no.nav.arbeidsgiver.min_side

import com.codahale.metrics.MetricRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

class Database private constructor(private val config: DatabaseConfig) : AutoCloseable {
    private val dataSource = HikariDataSource(config.asHikariConfig())

    private fun migrate() {
        Flyway.configure()
            .locations(config.migrationLocation)
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    suspend fun <T> transactional(block: suspend Connection.() -> T): T {
        return withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                try {
                    connection.autoCommit = false
                    val result = connection.block()
                    connection.commit()
                    result
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.close()
                }
            }
        }
    }

    suspend fun batchUpdate(
        @Language("PostgresSQL")
        query: String,
        batchData: List<Array<Any?>>
    ): IntArray {
        return transactional {
            prepareStatement(query).use { statement ->
                batchData.forEach { params ->
                    params.forEachIndexed { index, param ->
                        statement.setObject(index + 1, param)
                    }
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    suspend fun nonTransactionalExecuteUpdate(
        @Language("PostgresSQL")
        query: String,
        setup: ParameterSetters.() -> Unit = {},
    ): Int {
        dataSource.connection.use { connection ->
            return transactional {
                connection.executeUpdate(query, setup)
            }
        }
    }

    suspend fun <T> nonTransactionalExecuteQuery(
        @Language("PostgresSQL")
        query: String,
        setup: ParameterSetters.() -> Unit = {},
        transform: (rs: ResultSet) -> T
    ): List<T> {
        dataSource.connection.use { connection ->
            return transactional {
                connection.executeQuery(query, setup, transform)
            }
        }
    }

    companion object {
        fun openDatabase(config: DatabaseConfig): Database {
            val database = Database(config)
            database.migrate()
            return database
        }

        fun Connection.executeUpdate(
            @Language("PostgresSQL")
            query: String,
            setup: ParameterSetters.() -> Unit = {}
        ): Int {
            prepareStatement(query)
                .use { statement ->
                    ParameterSetters(statement).apply(setup)
                    return statement.executeUpdate()
                }
        }

        fun <T> Connection.executeQuery(
            @Language("PostgresSQL")
            query: String,
            setup: ParameterSetters.() -> Unit = {},
            transform: (rs: ResultSet) -> T
        ): List<T> {
            prepareStatement(query)
                .use { statement ->
                    ParameterSetters(statement).apply(setup)
                    statement.executeQuery().use { rs ->
                        val results = mutableListOf<T>()
                        while (rs.next()) {
                            results.add(transform(rs))
                        }
                        return results
                    }
                }
        }
    }

    override fun close() {
        dataSource.close()
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
            isAutoCommit = true
        }
    }
}

class ParameterSetters(
    private val preparedStatement: PreparedStatement,
) {
    private var index = 1


    fun <T : Enum<T>> enumAsText(value: T) = text(value.toString())

    fun text(value: String) = preparedStatement.setString(index++, value)
    fun nullableText(value: String?) = preparedStatement.setString(index++, value)
    fun integer(value: Int) = preparedStatement.setInt(index++, value)
    fun integer(value: Number) = preparedStatement.setInt(index++, value.toInt())
    fun double(value: Double) = preparedStatement.setDouble(index++, value)
    fun long(value: Long) = preparedStatement.setLong(index++, value)
    fun boolean(newState: Boolean) = preparedStatement.setBoolean(index++, newState)
    fun nullableBoolean(value: Boolean?) =
        if (value == null) preparedStatement.setNull(index++, Types.BOOLEAN) else boolean(value)

    fun uuid(value: UUID) = preparedStatement.setObject(index++, value)
    fun nullableUuid(value: UUID?) = preparedStatement.setObject(index++, value)

    /**
     * all timestamp values must be `truncatedTo` micros to avoid rounding/precision errors when writing and reading
     **/
    fun nullableTimestamptz(value: OffsetDateTime?) =
        preparedStatement.setObject(index++, value?.truncatedTo(ChronoUnit.MICROS))

    fun timestamp_without_timezone_utc(value: OffsetDateTime) =
        timestamp_without_timezone(value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())

    fun timestamp_without_timezone_utc(value: Instant) =
        timestamp_without_timezone(LocalDateTime.ofInstant(value, ZoneOffset.UTC))

    fun timestamp_without_timezone(value: LocalDateTime) =
        preparedStatement.setObject(index++, value.truncatedTo(ChronoUnit.MICROS))

    fun timestamp_with_timezone(value: OffsetDateTime) =
        preparedStatement.setObject(index++, value.truncatedTo(ChronoUnit.MICROS))

    fun bytea(value: ByteArray) = preparedStatement.setBytes(index++, value)
    fun byteaOrNull(value: ByteArray?) = preparedStatement.setBytes(index++, value)
    fun toInstantAsText(value: OffsetDateTime) = instantAsText(value.toInstant())
    fun instantAsText(value: Instant) = text(value.toString())
    fun offsetDateTimeAsText(value: OffsetDateTime) = text(value.toString())
    fun nullableInstantAsText(value: Instant?) = nullableText(value?.toString())

    fun nullableLocalDateTimeAsText(value: LocalDateTime?) = nullableText(value?.toString())
    fun localDateTimeAsText(value: LocalDateTime) = text(value.toString())
    fun nullableLocalDateAsText(value: LocalDate?) = nullableText(value?.toString())
    fun localDateAsText(value: LocalDate) = text(value.toString())

    fun nullableDate(value: LocalDate?) =
        preparedStatement.setDate(index++, value?.let { java.sql.Date.valueOf(it) })

    fun date(value: LocalDate) =
        preparedStatement.setDate(index++, value.let { java.sql.Date.valueOf(it) })


    fun nullableEnumAsTextList(value: List<Enum<*>>?) {
        nullableTextArray(value?.map { it.toString() })
    }

    fun textArray(value: List<String>) {
        val array = preparedStatement.connection.createArrayOf(
            "text",
            value.toTypedArray()
        )
        preparedStatement.setArray(index++, array)
    }

    fun nullableTextArray(value: List<String>?) {
        if (value == null) {
            preparedStatement.setArray(index++, value)
        } else {
            textArray(value)
        }
    }

    fun uuidArray(value: Collection<UUID>) {
        val array = preparedStatement.connection.createArrayOf(
            "uuid",
            value.toTypedArray()
        )
        preparedStatement.setArray(index++, array)
    }
}




