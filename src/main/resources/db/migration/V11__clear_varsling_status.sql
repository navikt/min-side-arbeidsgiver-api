create table poll_kontaktinfo
(
    virksomhetsnummer text not null primary key,
    poll_tidspunkt     text not null
);

create table kontaktinfo_resultat
(
    virksomhetsnummer text not null primary key,
    sjekket_tidspunkt text not null,
    har_epost         boolean,
    har_tlf           boolean
);


