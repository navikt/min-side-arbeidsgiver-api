create table sykefraværstatistikk_metadata_v2
(
    virksomhetsnummer text not null,
    arstall numeric not null,
    kvartal numeric not null,
    bransje text,
    næring text not null,
    primary key (virksomhetsnummer, arstall, kvartal)
);

create table sykefraværstatistikk_v2
(
    kode text not null,
    arstall numeric not null,
    kvartal numeric not null,
    kategori text not null,
    prosent decimal not null,
    primary key (kode, arstall, kvartal)
);