package no.nav.arbeidsgiver.min_side.tilgangssoknad

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.Database
import no.nav.arbeidsgiver.min_side.infrastruktur.SerializableInstant
import java.time.Instant
import java.util.UUID

@Serializable
data class DelegationRequestRow(
    val id: String,
    val orgnr: String,
    val resourceReferenceId: String,
    val status: String,
    val opprettet: SerializableInstant,
    val sistOppdatert: SerializableInstant,
)

class DelegationRequestRepository(
    private val database: Database,
) {
    suspend fun lagre(
        id: UUID,
        fnr: String,
        orgnr: String,
        resourceReferenceId: String,
        status: String,
    ) {
        database.nonTransactionalExecuteUpdate(
            """
            insert into delegation_request(id, fnr, orgnr, resource_reference_id, status)
            values (?, ?, ?, ?, ?)
            on conflict (id) do update set
                status = excluded.status,
                sist_oppdatert = now()
            """.trimIndent()
        ) {
            uuid(id)
            text(fnr)
            text(orgnr)
            text(resourceReferenceId)
            text(status)
        }
    }

    suspend fun oppdaterStatus(id: UUID, status: String) {
        database.nonTransactionalExecuteUpdate(
            """
            update delegation_request
               set status = ?, sist_oppdatert = now()
             where id = ?
            """.trimIndent()
        ) {
            text(status)
            uuid(id)
        }
    }

    suspend fun hentForBruker(fnr: String): List<DelegationRequestRow> =
        database.nonTransactionalExecuteQuery(
            """
            select id, orgnr, resource_reference_id, status, opprettet, sist_oppdatert
              from delegation_request
             where fnr = ?
             order by opprettet desc
            """.trimIndent(),
            { text(fnr) },
            { rs ->
                DelegationRequestRow(
                    id = rs.getString("id"),
                    orgnr = rs.getString("orgnr"),
                    resourceReferenceId = rs.getString("resource_reference_id"),
                    status = rs.getString("status"),
                    opprettet = rs.getTimestamp("opprettet").toInstant(),
                    sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant(),
                )
            }
        )
}
