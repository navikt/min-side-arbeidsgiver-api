create table storage
(
    fnr text not null,
    key text not null,
    version int not null,
    value text not null,
    timestamp text not null,
    primary key(fnr, key)
);



