package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import org.apache.commons.lang3.tuple.ImmutablePair
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification

import java.time.LocalDate

import static no.nav.arbeidsgiver.min_side.services.digisyfo.SykmeldingHendelse.create as createSykmelding

class DigisyfoRepositoryImplTest extends Specification {
    def leder1 = "10011223344"
    def leder2 = "20011223344"
    def ansatt1 = "10044332211"
    def ansatt2 = "20044332211"
    def ansatt3 = "30044332211"
    def vnr1 = "100111222"
    def vnr2 = "200111222"
    def vnr3 = "300111222"
    def uuid1 = "3608d78e-10a3-4179-9cac-000000000001"
    def uuid2 = "3608d78e-10a3-4179-9cac-000000000002"
    def uuid3 = "3608d78e-10a3-4179-9cac-000000000003"

    def "sletter ikke dagens sykmeldinger"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-01-01",
                nærmesteLedere: [[id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1]],
                sykmeldinger: [[id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
        ])

        expect:
        lookup(leder1) == [(vnr1): 1]
    }

    def "sletter gamle sykmeldinger"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-05-02",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt2, vnr: vnr2],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                        [id: "2", fnr: ansatt2, vnr: vnr2, dates: ["2020-01-02"]],
                ],
        ])

        expect:
        lookup(leder1) == [(vnr2): 0]
    }

    def "upsert bruker siste versjon"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                        [id: "1", fnr: ansatt1, vnr: vnr2, dates: ["2020-01-01"]],
                ]
        ])
        expect:
        lookup(leder1) == [(vnr2): 1]
    }

    def "tombstones fjerner sykmeldinger"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                        [id: "1"],
                ]
        ])
        expect:
        lookup(leder1) == [:]
    }

    def "bruker eldste tom-dato"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-05-02",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: [ "2020-01-01", "2020-05-02" ]],
                ],
        ])
        expect:
        lookup(leder1) == [(vnr1): 1]
    }

    def "tilgang selv uten aktiv sykmeldt"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2022-06-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt2, vnr: vnr2],
                        [id: uuid3, fnrLeder: leder1, fnrAnsatt: ansatt3, vnr: vnr3],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: [ "2022-01-01" ]],
                        [id: "2", fnr: ansatt2, vnr: vnr2, dates: [ "2022-03-01" ]],
                        [id: "3", fnr: ansatt3, vnr: vnr3, dates: [ "2022-07-01" ]],
                ]
        ])
        expect:
        lookup(leder1) == [(vnr2): 0, (vnr3): 1]
    }

    def "ser ikke ansatt som er sykmeldt i annen bedrift"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder2, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [ [id: "1", fnr: ansatt1, vnr: vnr2, dates: [ "2020-01-01" ]] ]
        ])
        expect:
        lookup(leder1) == [:]
        lookup(leder2) == [(vnr2): 1]
    }

    def "ser ikke ansatt i samme bedrift man ikke er leder for"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder2, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [ [id: "1", fnr: ansatt1, vnr: vnr2, dates: [ "2020-01-01" ]] ]
        ])
        expect:
        lookup(leder1) == [:]
        lookup(leder2) == [(vnr2): 1]
    }

    def "finner kun ansatt med aktiv sykmelding"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2022-11-07",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt2, vnr: vnr1]
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-01" ]],
                        [id: "2", fnr: ansatt2, vnr: vnr1, dates: [ "2022-11-21" ]]
                ]
        ])
        expect:
        lookup(leder1) == [(vnr1): 1]
    }

    def "to aktive sykmeldinger på en person gir en sykmeldt"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2022-11-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-01" ]],
                        [id: "2", fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-21" ]]
                ]
        ])
        expect:
        lookup(leder1) == [(vnr1): 1]
    }

    def "aktive sykmeldinger på forskjellige person holdes seperat"() {
        given:
        def lookup = prepareDatabaseSingletonBatches([
                today: "2022-11-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt2, vnr: vnr1],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-01" ]],
                        [id: "2", fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-21" ]],
                        [id: "3", fnr: ansatt2, vnr: vnr1, dates: [ "2022-11-01" ]],
                ]
        ])
        expect:
        lookup(leder1) == [(vnr1): 2]
    }

    def "batch: upsert – tombstone"() {
        given:
        def lookup = prepareDatabaseBatched([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                        [id: "1"],
                ]
        ])
        expect:
        lookup(leder1) == [:]
    }

    def "batch: upsert – upsert"() {
        given:
        def lookup = prepareDatabaseBatched([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                        [id: "1", fnr: ansatt1, vnr: vnr2, dates: ["2020-01-01"]],
                ]
        ])
        expect:
        lookup(leder1) == [(vnr2): 1]
    }

    def "batch: tombstone – upsert"() {
        given:
        def lookup = prepareDatabaseBatched([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [id: "1"],
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                ]
        ])
        expect:
        lookup(leder1) == [(vnr1): 1]
    }

    def "batch: upsert – tombstone – upsert"() {
        given:
        def lookup = prepareDatabaseBatched([
                today: "2020-01-01",
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [
                        [id: "1", fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]],
                        [id: "1"],
                        [id: "1", fnr: ansatt1, vnr: vnr2, dates: ["2020-01-01"]],
                ]
        ])
        expect:
        lookup(leder1) == [(vnr2): 1]
    }

    def prepareDatabaseSingletonBatches(setup) {
        prepareDatabase(
                setup.nærmesteLedere,
                setup.sykmeldinger.collect {
                    if (it.size() == 1) {
                        [ImmutablePair.of(it.id, null)]
                    } else {
                        [ImmutablePair.of(it.id, createSykmelding(it.fnr, it.vnr, it.dates))]
                    }
                },
                setup.today,
        )
    }

    def prepareDatabaseBatched(setup) {
        prepareDatabase(
                setup.nærmesteLedere,
                [
                        setup.sykmeldinger.collect {
                            if (it.size() == 1) {
                                ImmutablePair.of(it.id, null)
                            } else {
                                ImmutablePair.of(it.id, createSykmelding(it.fnr, it.vnr, it.dates))
                            }
                        }
                ],
                setup.today,
        )
    }

    def prepareDatabase(nærmesteLedere, batches, today) {
        PGSimpleDataSource ds = new PGSimpleDataSource()
        ds.setUrl("jdbc:postgresql://localhost:2345/postgres?user=postgres&password=postgres")

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .load()
        flyway.clean()
        flyway.migrate()
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds)

        DigisyfoRepository digisyfoRepository = new DigisyfoRepositoryImpl(jdbcTemplate, new LoggingMeterRegistry())

        nærmesteLedere.each {
            digisyfoRepository.processNærmesteLederEvent(
                    new NarmesteLederHendelse(
                            UUID.fromString(it.id),
                            it.fnrLeder,
                            null,
                            it.vnr,
                            it.fnrAnsatt
                    )
            )
        }
        batches.each {
            digisyfoRepository.processSykmeldingEvent(it)
        }

        digisyfoRepository.deleteOldSykmelding(LocalDate.parse(today))

        return { fnr ->
            digisyfoRepository.virksomheterOgSykmeldte(fnr, LocalDate.parse(today))
                .collectEntries {
                    [(it.virksomhetsnummer): it.antallSykmeldte]
            }
        }
    }
}


