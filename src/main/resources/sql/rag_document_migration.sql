create table if not exists tbl_rag_document (
    id bigserial primary key,
    file_id bigint not null references tbl_file(id),
    document_name varchar(255) not null,
    document_status varchar(30) not null default 'active',
    rag_status varchar(30) not null default 'pending',
    uploaded_by bigint not null,
    last_error text,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);
