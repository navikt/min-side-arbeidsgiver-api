package no.nav.arbeidsgiver.min_side.services.tiltak

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification

class RefusjonStatusRepositoryImplTest extends Specification {

    def jdbcTemplate = Mock(JdbcTemplate)
    def repo = new RefusjonStatusRepositoryImpl(new ObjectMapper(), jdbcTemplate)

    def "henter liste av statusoversikt"() {
        given:
        def virksomhetsnumre = ["42","44"]
        jdbcTemplate.queryForList(_ as String, virksomhetsnumre) >> [
                ["virksomhetsnummer": "42", "status": "a", "count": 10],
                ["virksomhetsnummer": "42", "status": "b", "count": 5],
                ["virksomhetsnummer": "44", "status": "a", "count": 22],
        ]

        expect:
        repo.statusoversikt(virksomhetsnumre).sort() == [
                new RefusjonStatusRepository.Statusoversikt("42", ["a": 10, "b": 5]),
                new RefusjonStatusRepository.Statusoversikt("44", ["a": 22]),
        ].sort()
    }
}
