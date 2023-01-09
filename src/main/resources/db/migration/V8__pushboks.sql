create table pushboks
(
    virksomhetsnummer text not null,
    tjeneste text not null,
    innhold text not null,

    primary key (virksomhetsnummer, tjeneste)
);


