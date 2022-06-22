create table naermeste_leder
(
    id                  uuid primary key,
    naermeste_leder_fnr varchar(32) not null
);

create index idx_naermeste_leder_naermeste_leder_fnr on naermeste_leder (naermeste_leder_fnr);

