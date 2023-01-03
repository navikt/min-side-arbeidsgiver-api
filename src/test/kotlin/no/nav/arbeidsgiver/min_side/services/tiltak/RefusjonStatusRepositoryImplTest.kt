package no.nav.arbeidsgiver.min_side.services.tiltak

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class RefusjonStatusRepositoryImplTest {

    val namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate::class.java)
    val jdbcTemplate = mock(JdbcTemplate::class.java)
    val repo = RefusjonStatusRepositoryImpl(ObjectMapper(), jdbcTemplate, namedParameterJdbcTemplate)

    @Test
    fun `henter liste av statusoversikt`() {
        val virksomhetsnumre = listOf("42", "44")
        `when`(namedParameterJdbcTemplate.queryForList(anyString(), eq(mapOf("virksomhetsnumre" to virksomhetsnumre))))
            .thenReturn(listOf(
                mapOf("virksomhetsnummer" to "42", "status" to "a", "count" to 10L),
                mapOf("virksomhetsnummer" to "42", "status" to "b", "count" to 5L),
                mapOf("virksomhetsnummer" to "44", "status" to "a", "count" to 22L),
            ))

        val result = repo.statusoversikt(virksomhetsnumre)
        val expected = listOf(
            RefusjonStatusRepository.Statusoversikt("42", mapOf("a" to 10, "b" to 5)),
            RefusjonStatusRepository.Statusoversikt("44", mapOf("a" to 22))
        )
        assertThat(result).containsAll(expected)
    }
}