package no.nav.arbeidsgiver.min_side.services.tiltak

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.Database


class RefusjonStatusRepository(
    private val database: Database,
) {
    data class Statusoversikt(
        val virksomhetsnummer: String = "",
        /**
         * Antall refusjoner per status. Kan være tomt map. Hvis det ikke er noen saker med gitt status,
         * så er ikke nødvendigvis statusen i mappet.
         */
        val statusoversikt: Map<String, Int> = mapOf()
    )

    suspend fun processHendelse(hendelse: RefusjonStatusHendelse) {
        database.nonTransactionalExecuteUpdate(
            """
            insert into refusjon_status(refusjon_id, virksomhetsnummer, avtale_id, status) 
                values(?, ?, ?, ?) 
            on conflict (refusjon_id) 
                do update set 
                    virksomhetsnummer = EXCLUDED.virksomhetsnummer,        
                    avtale_id = EXCLUDED.avtale_id,        
                    status = EXCLUDED.status;
            """.trimIndent()
        ) {
            text(hendelse.refusjonId)
            text(hendelse.virksomhetsnummer)
            text(hendelse.avtaleId)
            text(hendelse.status)
        }
    }

    suspend fun statusoversikt(virksomhetsnummer: Collection<String>): List<Statusoversikt> {
        if (virksomhetsnummer.isEmpty()) {
            return listOf()
        }
        val virksomhetsnummere = virksomhetsnummer.joinToString(",") { "?" }

        val grouped = database.nonTransactionalExecuteQuery(
            """
        select virksomhetsnummer, status, count(*) as count 
            from refusjon_status 
            where virksomhetsnummer in ($virksomhetsnummere) 
            group by virksomhetsnummer, status
        """.trimIndent(),
            { virksomhetsnummer.forEach { text(it) } },
            { rs ->
                mapOf(
                    "virksomhetsnummer" to rs.getString("virksomhetsnummer"),
                    "status" to rs.getString("status"),
                    "count" to rs.getLong("count")
                )
            }
        )
            .groupBy { it["virksomhetsnummer"] }
            .mapValues {
                it.value
                    .associateBy { v -> v["status"] as String }
                    .mapValues { v -> (v.value["count"] as Long).toInt() }
            }
        return grouped.map { Statusoversikt(it.key as String, it.value) }
    }
}


@Serializable
data class RefusjonStatusHendelse(
    @SerialName("refusjonId") val refusjonId: String,
    @SerialName("bedriftNr") val virksomhetsnummer: String,
    @SerialName("avtaleId") val avtaleId: String,
    @SerialName("status") val status: String
)