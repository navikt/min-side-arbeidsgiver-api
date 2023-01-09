package no.nav.arbeidsgiver.min_side.services.pushboks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksRepository.Pushboks
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksRepository.PushboksKey
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

interface PushboksRepository {
    data class PushboksKey(
        val tjeneste: String,
        val virksomhetsnummer: String,
    )

    data class Pushboks(
        val virksomhetsnummer: String,
        val tjeneste: String,
        val innhold: JsonNode,
    )

    fun hent(virksomhetsnummer: String): List<Pushboks>
    fun processEvent(key: PushboksKey, innhold: String?)
}

@Profile(
    "dev-gcp",
    "prod-gcp",
    "local-kafka",
)
@Repository
class PushboksRepositoryImpl(
    val objectMapper: ObjectMapper,
    val jdbcTemplate: NamedParameterJdbcTemplate,
) : PushboksRepository {
    override fun hent(virksomhetsnummer: String): List<Pushboks> {
        jdbcTemplate.queryForStream(
            """
            select
                virksomhetsnummer, tjeneste, innhold
            from pushboks
                where virksomhetsnummer = :virksomhetsnummer 
            """,
            mapOf(
                "virksomhetsnummer" to virksomhetsnummer
            )
        ) { rs: ResultSet, _: Int ->
            Pushboks(
                virksomhetsnummer = rs.getString("virksomhetsnummer"),
                tjeneste = rs.getString("tjeneste"),
                innhold = objectMapper.readValue(rs.getString("innhold")),
            )
        }.use { stream -> return stream.toList() }
    }

    override fun processEvent(key: PushboksKey, innhold: String?) {
        // TODO: handle "null" body
        if (innhold == null) {
            jdbcTemplate.update("""
                delete from pushboks 
                where virksomhetsnummer = :virksomhetsnummer and tjeneste = :tjeneste
            """.trimIndent(),
                mapOf(
                    "virksomhetsnummer" to key.virksomhetsnummer,
                    "tjeneste" to key.tjeneste
                )
            )
        } else {
            jdbcTemplate.update(
                """
                insert into pushboks(virksomhetsnummer, tjeneste, innhold)  
                values(:virksomhetsnummer, :tjeneste, :innhold)  
                on conflict(virksomhetsnummer, tjeneste) 
                do 
                update set 
                    innhold = EXCLUDED.innhold;
                """.trimIndent(),
                mapOf(
                    "virksomhetsnummer" to key.virksomhetsnummer,
                    "tjeneste" to key.tjeneste,
                    "innhold" to innhold,
                )
            )
        }
    }
}

@ConditionalOnMissingBean(PushboksRepository::class)
@Profile(
    "local",
    "labs",
)
@Repository
class PushboksRepositoryStub(
    val objectMapper: ObjectMapper,
) : PushboksRepository {
    val pushbokser = mutableMapOf<String, MutableList<Pushboks>>()

    private fun pushbokser(virksomhetsnummer: String) = pushbokser.computeIfAbsent(virksomhetsnummer) {mutableListOf() }

    override fun hent(virksomhetsnummer: String): List<Pushboks> = pushbokser(virksomhetsnummer)

    override fun processEvent(key: PushboksKey, innhold: String?) {
        pushbokser(key.virksomhetsnummer).removeIf { it.virksomhetsnummer == key.virksomhetsnummer && it.tjeneste == key.tjeneste }

        if (innhold != null) {
            pushbokser(key.virksomhetsnummer).add(Pushboks(key.virksomhetsnummer, key.tjeneste, objectMapper.convertValue(innhold)))
        }
    }

}