package no.nav.arbeidsgiver.min_side.services.digisyfo

import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import org.apache.commons.lang3.tuple.ImmutablePair
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification

import java.time.LocalDate

import static no.nav.arbeidsgiver.min_side.services.digisyfo.SykmeldingHendelse.create as createSykmelding

class SykmeldingRepositoryImplTest extends Specification {
    def leder1 = "10011223344"
    def leder2 = "20011223344"
    def ansatt1 = "10044332211"
    def ansatt2 = "20044332211"
    def vnr1 = "100111222"
    def vnr2 = "200111222"
    def uuid1 = "3608d78e-10a3-4179-9cac-000000000001"
    def uuid2 = "3608d78e-10a3-4179-9cac-000000000002"


    def "sletter ikke dagens sykmeldinger"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [[id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1]],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                ],
                deleteFrom: LocalDate.parse("2020-01-01"),
        ])

        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr1): 1]
    }

    def "sletter gamle sykmeldinger"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt2, vnr: vnr2],
                ],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                        [key: "2", event: [fnr: ansatt2, vnr: vnr2, dates: ["2020-01-02"]]],
                ],
                deleteFrom: LocalDate.parse("2020-05-02"),
        ])

        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr2): 1]
    }

    def "upsert bruker siste versjon"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                        [key: "1", event: [fnr: ansatt1, vnr: vnr2, dates: ["2020-01-01"]]],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr2): 1]
    }

    def "tombstones fjerner sykmeldinger"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                        [key: "1", event: null],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [:]
    }

    def "bruker eldste tom-dato"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [key: "1", event: [
                                fnr: ansatt1,
                                vnr: vnr1,
                                dates: [ "2020-01-01", "2020-05-02" ]]],
                ],
                deleteFrom: LocalDate.parse("2020-05-02"),
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr1): 1]
    }

    def "ser ikke ansatt som er sykmeldt i annen bedrift"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder2, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [ [key: "1", event: [ fnr: ansatt1, vnr: vnr2, dates: [ "2020-01-01" ]]] ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [:]
        sykmeldingRepository.oversiktSykmeldinger(leder2) == [(vnr2): 1]
    }

    def "ser ikke ansatt i samme bedrift man ikke er leder for"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder2, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [ [key: "1", event: [ fnr: ansatt1, vnr: vnr2, dates: [ "2020-01-01" ]]] ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [:]
        sykmeldingRepository.oversiktSykmeldinger(leder2) == [(vnr2): 1]
    }

    def "to aktive sykmeldinger på en person gir en sykmeldt"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [key: "1", event: [ fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-01" ]]],
                        [key: "2", event: [ fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-21" ]]]
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr1): 2]
    }

    def "aktive sykmeldinger på forskjellige person holdes seperat"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseSingletonBatches([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt2, vnr: vnr1],
                ],
                sykmeldinger: [
                        [key: "1", event: [ fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-01" ]]],
                        [key: "2", event: [ fnr: ansatt1, vnr: vnr1, dates: [ "2022-11-21" ]]],
                        [key: "3", event: [ fnr: ansatt2, vnr: vnr1, dates: [ "2022-11-01" ]]],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr1): 3]
    }

    def "batch: upsert – tombstone"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseBatched([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                        [key: "1", event: null],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [:]
    }

    def "batch: upsert – upsert"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseBatched([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                        [key: "1", event: [fnr: ansatt1, vnr: vnr2, dates: ["2020-01-01"]]],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr2): 1]
    }

    def "batch: tombstone – upsert"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseBatched([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                ],
                sykmeldinger: [
                        [key: "1", event: null],
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr1): 1]
    }

    def "batch: upsert – tombstone – upsert"() {
        given:
        SykmeldingRepositoryImpl sykmeldingRepository = prepareDatabaseBatched([
                nærmesteLedere: [
                        [id: uuid1, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr1],
                        [id: uuid2, fnrLeder: leder1, fnrAnsatt: ansatt1, vnr: vnr2],
                ],
                sykmeldinger: [
                        [key: "1", event: [fnr: ansatt1, vnr: vnr1, dates: ["2020-01-01"]]],
                        [key: "1", event: null],
                        [key: "1", event: [fnr: ansatt1, vnr: vnr2, dates: ["2020-01-01"]]],
                ]
        ])
        expect:
        sykmeldingRepository.oversiktSykmeldinger(leder1) == [(vnr2): 1]
    }

    def prepareDatabaseSingletonBatches(setup) {
        prepareDatabase(
                setup.nærmesteLedere,
                setup.sykmeldinger.collect {
                    if (it.event == null) {
                        [ImmutablePair.of(it.key, null)]
                    } else {
                        [ImmutablePair.of(it.key, createSykmelding(it.event.fnr, it.event.vnr, it.event.dates))]
                    }
                },
                setup.deleteFrom
        )
    }

    def prepareDatabaseBatched(setup) {
        prepareDatabase(
                setup.nærmesteLedere,
                [
                        setup.sykmeldinger.collect {
                            if (it.event == null) {
                                ImmutablePair.of(it.key, null)
                            } else {
                                ImmutablePair.of(it.key, createSykmelding(it.event.fnr, it.event.vnr, it.event.dates))
                            }
                        }
                ],
                null
        )
    }

    def prepareDatabase(nærmesteLedere, batches, deleteFrom) {
        PGSimpleDataSource ds = new PGSimpleDataSource()
        ds.setUrl("jdbc:postgresql://localhost:2345/postgres?user=postgres&password=postgres")

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .load()
        flyway.clean()
        flyway.migrate()
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds)

        DigisyfoRepository digisyfoRepository = new DigisyfoRepositoryImpl(jdbcTemplate, new LoggingMeterRegistry())
        SykmeldingRepository sykmeldingRepository = new SykmeldingRepositoryImpl(jdbcTemplate)

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

        if (deleteFrom != null) {
            digisyfoRepository.deleteOldSykmelding(deleteFrom)
        }

        return sykmeldingRepository
    }
}

