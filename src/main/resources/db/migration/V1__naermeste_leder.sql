create table naermeste_leder
(
    id                  text primary key,
    naermeste_leder_fnr text not null
);

create index idx_naermeste_leder_naermeste_leder_fnr on naermeste_leder (naermeste_leder_fnr);

