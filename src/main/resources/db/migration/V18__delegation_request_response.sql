alter table delegation_request
    add column details_link text,
    add column last_response jsonb;
