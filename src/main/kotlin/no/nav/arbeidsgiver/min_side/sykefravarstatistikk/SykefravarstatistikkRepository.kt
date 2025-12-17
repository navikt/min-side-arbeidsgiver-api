package no.nav.arbeidsgiver.min_side.sykefravarstatistikk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.Database

data class Statistikk(
    val kategori: String,
    val kode: String,
    val prosent: Double,
)

class SykefravarstatistikkRepository(
    private val database: Database,
) {

    suspend fun virksomhetstatistikk(virksomhetsnummer: String) = database.nonTransactionalExecuteQuery(
        """
        select kategori, kode, prosent
            from sykefraværstatistikk_v2
            where kategori = 'VIRKSOMHET' and kode = ?
            order by arstall desc, kvartal desc
        """.trimIndent(),
        {
            text(virksomhetsnummer)
        },
        { rs ->
            Statistikk(
                kategori = rs.getString("kategori"),
                kode = rs.getString("kode"),
                prosent = rs.getDouble("prosent")
            )
        }
    ).firstOrNull()


    suspend fun statistikk(virksomhetsnummer: String) = database.nonTransactionalExecuteQuery(
        //-- TODO: vurder eksplisitte felt i stedet for coalesce her, kan by på problemer
        """  
        select 
          coalesce(sb.kategori, sn.kategori) as kategori, 
          coalesce(sb.kode, sn.kode) as kode, 
          coalesce(sb.prosent, sn.prosent) as prosent
        from sykefraværstatistikk_metadata_v2 meta
          left join sykefraværstatistikk_v2 sb on (sb.kode = meta.bransje and sb.arstall = meta.arstall and sb.kvartal = meta.kvartal)
          left join sykefraværstatistikk_v2 sn on (sn.kode = meta.næring and sn.arstall = meta.arstall and sn.kvartal = meta.kvartal)
        where meta.virksomhetsnummer = ?
          and coalesce(sb.prosent, sn.prosent) is not null
        order by meta.arstall desc, meta.kvartal desc, kategori
        """.trimIndent(),
        { text(virksomhetsnummer) },
        { rs ->
            Statistikk(
                kategori = rs.getString("kategori"),
                kode = rs.getString("kode"),
                prosent = rs.getDouble("prosent")
            )
        }
    ).firstOrNull()

    suspend fun processMetadataVirksomhet(metadata: MetadataVirksomhetDto) {
        database.nonTransactionalExecuteUpdate(
            """
            insert into sykefraværstatistikk_metadata_v2(virksomhetsnummer, bransje, næring, arstall, kvartal) 
                values(?, ?, ?, ?, ?)
            on conflict (virksomhetsnummer, arstall, kvartal) 
                do update set 
                    virksomhetsnummer = EXCLUDED.virksomhetsnummer,        
                    bransje = EXCLUDED.bransje,        
                    næring = EXCLUDED.næring;
            """.trimIndent()
        ) {
            text(metadata.virksomhetsnummer)
            nullableText(metadata.bransje)
            text(metadata.naring)
            integer(metadata.arstall)
            integer(metadata.kvartal)
        }
    }

    suspend fun processStatistikkategori(statistikkategori: StatistikkategoriDto) {
        database.nonTransactionalExecuteUpdate(
            """
            insert into sykefraværstatistikk_v2(kode, kategori, prosent, arstall, kvartal) 
                values(?, ?, ?, ?, ?)
            on conflict (kode, arstall, kvartal) 
                do update set 
                    kode = EXCLUDED.kode,        
                    kategori = EXCLUDED.kategori,        
                    prosent = EXCLUDED.prosent;
            """.trimIndent()
        ) {
            text(statistikkategori.kode)
            text(statistikkategori.kategori)
            double(statistikkategori.prosent)
            integer(statistikkategori.arstall)
            integer(statistikkategori.kvartal)
        }
    }
}

/**
 * dataminimert til det vi trenger
 * kilde: https://github.com/navikt/sykefravarsstatistikk-api/blob/master/src/main/java/no/nav/arbeidsgiver/sykefravarsstatistikk/api/infrastruktur/kafka/dto/MetadataVirksomhetKafkamelding.kt
 */
@Serializable
data class MetadataVirksomhetDto(
    @SerialName("orgnr") val virksomhetsnummer: String,
    @SerialName("naring") val naring: String,
    @SerialName("bransje") val bransje: String? = null,
    @SerialName("arstall") val arstall: Int,
    @SerialName("kvartal") val kvartal: Int,
)

@Serializable
data class MetadataVirksomhetKafkaKeyDto(
    @SerialName("orgnr") val virksomhetsnummer: String,
    @SerialName("arstall") val arstall: Int,
    @SerialName("kvartal") val kvartal: Int,
)

/**
 * dataminimert til det vi trenger
 * kilde: https://github.com/navikt/sykefravarsstatistikk-api/blob/master/src/main/java/no/nav/arbeidsgiver/sykefravarsstatistikk/api/infrastruktur/kafka/dto/StatistikkategoriKafkamelding.kt
 */
@Serializable
data class StatistikkategoriDto(
    @SerialName("kategori") val kategori: String,
    @SerialName("kode") val kode: String, // orgnr dersom kategori er VIRKSOMHET, bransje dersom kategori er BRANSJE, osv
    @SerialName("siste4Kvartal") val siste4Kvartal: ProsentWrapper? = null,
    @SerialName("sistePubliserteKvartal") val sistePubliserteKvartal: ÅrstallKvartalWrapper,
) {
    val prosent: Double
        get() = siste4Kvartal?.prosent ?: 0.0

    val arstall: Number
        get() = sistePubliserteKvartal.arstall

    val kvartal: Number
        get() = sistePubliserteKvartal.kvartal

    @Serializable
    data class ProsentWrapper(
        @SerialName("prosent") val prosent: Double? = null,
    )

    @Serializable
    data class ÅrstallKvartalWrapper(
        @SerialName("årstall") val arstall: Int,
        @SerialName("kvartal") val kvartal: Int,
    )
}

@Serializable
data class StatistikkategoriKafkaKeyDto(
    @SerialName("kategori") val kategori: String,
    @SerialName("kode") val kode: String,
    @SerialName("årstall") val arstall: Int,
    @SerialName("kvartal") val kvartal: Int,
)