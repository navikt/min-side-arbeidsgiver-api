create table refusjon_status
(
    refusjon_id text primary key,
    virksomhetsnummer text not null,
    avtale_id text not null,
    status text not null
);

create index idx_refusjon_status_virksomhetsnummer on refusjon_status (virksomhetsnummer);

