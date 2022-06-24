package no.nav.arbeidsgiver.min_side.services.tiltak

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import spock.lang.Specification

class RefusjonStatusRepositoryImplTest extends Specification {

    def namedParameterJdbcTemplate = Mock(NamedParameterJdbcTemplate)
    def repo = new RefusjonStatusRepositoryImpl(new ObjectMapper(), Mock(JdbcTemplate), namedParameterJdbcTemplate)

    def "henter liste av statusoversikt"() {
        given:
        def virksomhetsnumre = ["42","44"]
        namedParameterJdbcTemplate.queryForList(_ as String, [virksomhetsnumre: virksomhetsnumre]) >> [
                ["virksomhetsnummer": "42", "status": "a", "count": 10L],
                ["virksomhetsnummer": "42", "status": "b", "count": 5L],
                ["virksomhetsnummer": "44", "status": "a", "count": 22L],
        ]

        expect:
        repo.statusoversikt(virksomhetsnumre).sort() == [
                new RefusjonStatusRepository.Statusoversikt("42", ["a": 10, "b": 5]),
                new RefusjonStatusRepository.Statusoversikt("44", ["a": 22]),
        ].sort()
    }
}
