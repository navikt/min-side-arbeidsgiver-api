package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.arbeidsgiver.min_side.Database

data class Statistikk(
    val kategori: String,
    val kode: String,
    val prosent: Double,
)

class SykefraværstatistikkRepository(
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
            """.trimIndent(),
            {
                text(metadata.virksomhetsnummer)
                nullableText(metadata.bransje)
                text(metadata.næring)
                integer(metadata.arstall)
                integer(metadata.kvartal)
            }
        )
    }

    suspend fun processStatistikkategori(statistikkategori: StatistikkategoriDto) {
        database.nonTransactionalExecuteUpdate(
            """
            insert into sykefraværstatistikk_v2(kode, kategori, prosent, arstall, kvartal) 
                values(:kode, :kategori, :prosent, :arstall, :kvartal)
            on conflict (kode, arstall, kvartal) 
                do update set 
                    kode = EXCLUDED.kode,        
                    kategori = EXCLUDED.kategori,        
                    prosent = EXCLUDED.prosent;
            """.trimIndent(),
            {
                text(statistikkategori.kode)
                text(statistikkategori.kategori)
                double(statistikkategori.prosent)
                integer(statistikkategori.arstall)
                integer(statistikkategori.kvartal)
            }
        )
    }
}

/**
 * dataminimert til det vi trenger
 * kilde: https://github.com/navikt/sykefravarsstatistikk-api/blob/master/src/main/java/no/nav/arbeidsgiver/sykefravarsstatistikk/api/infrastruktur/kafka/dto/MetadataVirksomhetKafkamelding.kt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MetadataVirksomhetDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("orgnr") val virksomhetsnummer: String,
    @param:JsonProperty("naring") val næring: String,
    @param:JsonProperty("bransje") val bransje: String?,
    @param:JsonProperty("arstall") val arstall: Number,
    @param:JsonProperty("kvartal") val kvartal: Number,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetadataVirksomhetKafkaKeyDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("orgnr") val virksomhetsnummer: String,
    @param:JsonProperty("arstall") val arstall: Number,
    @param:JsonProperty("kvartal") val kvartal: Number,
)

/**
 * dataminimert til det vi trenger
 * kilde: https://github.com/navikt/sykefravarsstatistikk-api/blob/master/src/main/java/no/nav/arbeidsgiver/sykefravarsstatistikk/api/infrastruktur/kafka/dto/StatistikkategoriKafkamelding.kt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StatistikkategoriDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("kategori") val kategori: String,
    @param:JsonProperty("kode") val kode: String, // orgnr dersom kategori er VIRKSOMHET, bransje dersom kategori er BRANSJE, osv
    @param:JsonProperty("siste4Kvartal") val siste4Kvartal: ProsentWrapper?,
    @param:JsonProperty("sistePubliserteKvartal") val sistePubliserteKvartal: ÅrstallKvartalWrapper,
) {
    val prosent: Double
        get() = siste4Kvartal?.prosent ?: 0.0

    val arstall: Number
        get() = sistePubliserteKvartal.arstall

    val kvartal: Number
        get() = sistePubliserteKvartal.kvartal

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ProsentWrapper @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
        @param:JsonProperty("prosent") val prosent: Double,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ÅrstallKvartalWrapper @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
        @param:JsonProperty("årstall") val arstall: Number,
        @param:JsonProperty("kvartal") val kvartal: Number,
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatistikkategoriKafkaKeyDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("kategori") val kategori: String,
    @param:JsonProperty("kode") val kode: String,
    @param:JsonProperty("årstall") val arstall: Number,
    @param:JsonProperty("kvartal") val kvartal: Number,
)