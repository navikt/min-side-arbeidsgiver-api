create table sykmelding
(
    id text primary key,
    virksomhetsnummer text not null,
    ansatt_fnr text not null,
    -- val latestTom = sykmeldingArbeidsgiverKafkaMessage.sykmelding.sykmeldingsperioder.maxOf { it.tom }
    sykmeldingsperiode_slutt date
);


-- for deleteion based on time
create index sykmelding_sykmeldingsperiode_slutt_idx
on sykmelding(sykmeldingsperiode_slutt);

-- for oppslag på join mot nærmesteleder-tabell.
create index sykmelding_fnr_vnr_idx
on sykmelding(virksomhetsnummer, ansatt_fnr);


