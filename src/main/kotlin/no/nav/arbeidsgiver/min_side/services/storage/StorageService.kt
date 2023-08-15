package no.nav.arbeidsgiver.min_side.services.storage

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement

@Service
class StorageService(val jdbcTemplate: JdbcTemplate) {
    fun delete(key: String, fnr: String, version: Int?): StorageEntry? {
        val existing = get(key, fnr, true) ?: return null

        if (version != null && existing.version != version) {
            throw StaleStorageException(
                "Supplied version($version) does not match current version(${existing.version})",
                existing
            )
        }

        jdbcTemplate.update(
            "delete from storage where key = ? and fnr = ?",
            key,
            fnr,
        )
        return existing
    }

    @Transactional
    fun put(key: String, fnr: String, value: String, version: Int?): StorageEntry? {
        val existing = get(key, fnr, true)
        if (existing == null) {
            jdbcTemplate.update(
                "insert into storage(key, fnr, value, version, timestamp) values (?, ?, ?, 1, now())",
                key,
                fnr,
                value,
            )
        } else {
            if (version != null && existing.version != version) {
                throw StaleStorageException(
                    "Supplied version($version) does not match current version(${existing.version})",
                    existing
                )
            }

            jdbcTemplate.update(
                "update storage set value = ?, version = version + 1, timestamp = now() where key = ? and fnr = ?",
                value,
                key,
                fnr,
            )
        }
        return get(key, fnr)
    }

    fun get(key: String, fnr: String, forUpdate: Boolean = false): StorageEntry? {
        return jdbcTemplate.query(
            "select * from storage where key = ? and fnr = ? ${if (forUpdate) "for update" else ""}",
            { ps: PreparedStatement ->
                ps.setString(1, key)
                ps.setString(2, fnr)
            },
        ) { rs, _ ->
            StorageEntry(
                key = rs.getString("key"),
                fnr = rs.getString("fnr"),
                value = rs.getString("value"),
                version = rs.getInt("version"),
                timestamp = rs.getString("timestamp"),
            )
        }.firstOrNull()
    }
}

class StaleStorageException(message: String, val existing: StorageEntry) : RuntimeException(message)

data class StorageEntry(
    val key: String,
    val fnr: String,
    val value: String,
    val version: Int,
    val timestamp: String,
)
