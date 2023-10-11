create table varsling_status
(
    varsel_id text not null primary key,
    virksomhetsnummer text not null,
    status text not null,
    status_tidspunkt text not null,
    varslet_tidspunkt text not null
);
create index varsling_status_virksomhetsnummer_idx on varsling_status (virksomhetsnummer);



