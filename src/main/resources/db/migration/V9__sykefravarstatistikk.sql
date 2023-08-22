create table sykefraværstatistikk_metadata
(
    virksomhetsnummer text not null primary key,
    bransje text,
    næring text not null
);

create table sykefraværstatistikk
(
    kode text not null primary key,
    kategori text not null,
    prosent decimal not null
);


