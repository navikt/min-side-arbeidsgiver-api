package no.nav.arbeidsgiver.min_side.services.digisyfo

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.stream.Stream

interface SykmeldingRepository {
    fun oversiktSykmeldinger(nærmestelederFnr: String): Map<String, Int>
}

@Profile("dev-gcp", "prod-gcp")
@Repository
class SykmeldingRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : SykmeldingRepository {
    override fun oversiktSykmeldinger(nærmestelederFnr: String): Map<String, Int> {
        jdbcTemplate.queryForStream<Pair<String, Int>>(
            """
                with nl as (
                    select virksomhetsnummer, ansatt_fnr from naermeste_leder where naermeste_leder_fnr = ?
                )
                select s.virksomhetsnummer as virksomhetsnummer, count(*) as antall
                from sykmelding as s
                join nl on
                nl.virksomhetsnummer = s.virksomhetsnummer and
                nl.ansatt_fnr = s.ansatt_fnr
                group by s.virksomhetsnummer
            """,
            { ps: PreparedStatement -> ps.setString(1, nærmestelederFnr) },
            { rs: ResultSet, _: Int ->
                rs.getString("virksomhetsnummer") to rs.getInt("antall")
            }
        ).use { stream : Stream<Pair<String, Int>> ->
            return stream.toList().toMap()
        }
    }
}


@Profile("local", "demo")
@Repository
class SykmeldingRepositoryStub : SykmeldingRepository {
    override fun oversiktSykmeldinger(nærmestelederFnr: String): Map<String, Int> {
        return mapOf("910825526" to 4)
    }
}
