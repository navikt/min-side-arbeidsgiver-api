package no.nav.arbeidsgiver.min_side.services.digisyfo

import no.nav.arbeidsgiver.min_side.Database
import java.sql.ResultSet

interface SykmeldingRepository {
    suspend fun oversiktSykmeldinger(nærmestelederFnr: String): Map<String, Int>
}

class SykmeldingRepositoryImpl(
    private val database: Database
) : SykmeldingRepository {
    override suspend fun oversiktSykmeldinger(nærmestelederFnr: String): Map<String, Int> {
        return database.nonTransactionalExecuteQuery(
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
            { text(nærmestelederFnr) },
            { rs: ResultSet ->
                rs.getString("virksomhetsnummer") to rs.getInt("antall")
            }
        ).toMap()
    }
}


class SykmeldingRepositoryStub : SykmeldingRepository {
    override suspend fun oversiktSykmeldinger(nærmestelederFnr: String): Map<String, Int> {
        return mapOf("910825526" to 4)
    }
}
