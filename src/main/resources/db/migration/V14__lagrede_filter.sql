create table lagrede_filter (
    filter_id text not null primary key,
    fnr text not null,
    navn text not null,
    side integer not null,
    tekstsoek text,
    virksomheter text,
    sortering text not null,
    sakstyper text,
    oppgave_filter text,
    opprettet_tidspunkt timestamp not null,
    sist_endret_tidspunkt timestamp not null
);

create index lagrede_filter_fnr_idx on lagrede_filter (fnr);