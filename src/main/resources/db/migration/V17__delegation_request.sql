create table delegation_request (
    id                    uuid        primary key,
    fnr                   text        not null,
    orgnr                 text        not null,
    resource_reference_id text        not null,
    status                text        not null,
    opprettet             timestamptz not null default now(),
    sist_oppdatert        timestamptz not null default now()
);

create index delegation_request_fnr_idx on delegation_request (fnr);
